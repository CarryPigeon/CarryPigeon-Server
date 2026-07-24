package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * Core:Forward 消息插件。
 * 职责：校验转发附言与单条、合并来源快照，并生成 canonical data 和 preview。
 * 边界：来源快照由转发领域服务生成；客户端不能通过通用消息入口直接发送该 domain。
 */
public class ForwardChannelMessagePlugin implements ChannelMessagePlugin {

    @Override
    public String supportedType() {
        return "forward";
    }

    @Override
    public String supportedDomain() {
        return "Core:Forward";
    }

    @Override
    public boolean clientSendable() {
        return false;
    }

    @Override
    public CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> rawData
    ) {
        MessagePluginDataReader.requireVersion(domainVersion, "1.0.0");
        Map<String, Object> data = MessagePluginDataReader.copyData(rawData);
        String nestedDomain = MessagePluginDataReader.requiredString(data, "domain", "domain must not be blank");
        if (!"Core:Text".equals(nestedDomain)) {
            throw ProblemException.validationFailed("forward content domain is not supported");
        }
        String nestedVersion = MessagePluginDataReader.requiredString(
                data,
                "domain_version",
                "domain_version must not be blank"
        );
        MessagePluginDataReader.requireVersion(nestedVersion, "1.0.0");

        Map<String, Object> canonicalData = new LinkedHashMap<>();
        canonicalData.put("domain", nestedDomain);
        canonicalData.put("domain_version", nestedVersion);
        String comment = canonicalComment(data, canonicalData);

        Map<String, Object> forwardedFrom = MessagePluginDataReader.optionalObject(data, "forwarded_from");
        List<Map<String, Object>> forwardedMessages = optionalSources(data.get("forwarded_messages"));
        if ((forwardedFrom == null) == (forwardedMessages == null)) {
            throw ProblemException.validationFailed("exactly one forward source field is required");
        }
        if (forwardedFrom != null) {
            canonicalData.put("forwarded_from", canonicalSource(forwardedFrom));
            return new CanonicalData(canonicalData, comment == null ? "转发消息" : comment);
        }
        if (forwardedMessages.size() < 2) {
            throw ProblemException.validationFailed("forwarded_messages must contain at least two sources");
        }
        List<Map<String, Object>> canonicalSources = forwardedMessages.stream()
                .map(this::canonicalSource)
                .toList();
        canonicalData.put("forwarded_messages", canonicalSources);
        String preview = comment == null ? "转发 " + canonicalSources.size() + " 条消息" : comment;
        return new CanonicalData(canonicalData, preview);
    }

    private String canonicalComment(Map<String, Object> data, Map<String, Object> canonicalData) {
        Map<String, Object> content = MessagePluginDataReader.optionalObject(data, "content");
        if (content == null) {
            return null;
        }
        String text = MessagePluginDataReader.requiredString(content, "text", "content.text must not be blank");
        canonicalData.put("content", Map.of("text", text));
        return text;
    }

    private List<Map<String, Object>> optionalSources(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> rawList)) {
            throw ProblemException.validationFailed("forwarded_messages must be array");
        }
        List<Map<String, Object>> sources = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw ProblemException.validationFailed("forwarded_messages must contain objects");
            }
            Map<String, Object> source = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw ProblemException.validationFailed("forwarded_messages must contain string keys");
                }
                source.put(key, entry.getValue());
            }
            sources.add(source);
        }
        return sources;
    }

    private Map<String, Object> canonicalSource(Map<String, Object> source) {
        String messageId = MessagePluginDataReader.requiredSnowflake(source.get("mid"), "forward source mid");
        Boolean unavailable = MessagePluginDataReader.optionalBoolean(source, "unavailable");
        if (Boolean.TRUE.equals(unavailable)) {
            return Map.of("mid", messageId, "unavailable", true);
        }
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("mid", messageId);
        canonical.put("cid", MessagePluginDataReader.requiredSnowflake(source.get("cid"), "forward source cid"));
        canonical.put("uid", MessagePluginDataReader.requiredSnowflake(source.get("uid"), "forward source uid"));
        Object preview = source.get("preview");
        if (!(preview instanceof String previewText)) {
            throw ProblemException.validationFailed("forward source preview must be string");
        }
        canonical.put("preview", previewText);
        canonical.put("send_time", MessagePluginDataReader.requiredPositiveLong(
                source,
                "send_time",
                "forward source send_time must be greater than 0"
        ));
        return canonical;
    }
}
