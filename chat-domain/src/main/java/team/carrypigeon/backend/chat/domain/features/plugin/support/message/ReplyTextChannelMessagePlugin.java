package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * Core:ReplyText 消息插件。
 * 职责：校验回复、内联引用和文本内容，并生成规范化 canonical data。
 * 边界：mentions 仍由消息级元数据处理，不允许进入本插件 data。
 */
public class ReplyTextChannelMessagePlugin implements ChannelMessagePlugin {

    @Override
    public String supportedType() {
        return "reply-text";
    }

    @Override
    public String supportedDomain() {
        return "Core:ReplyText";
    }

    @Override
    public CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> rawData
    ) {
        MessagePluginDataReader.requireVersion(domainVersion, "1.0.0");
        Map<String, Object> data = MessagePluginDataReader.copyData(rawData);
        Map<String, Object> content = MessagePluginDataReader.requiredObject(data, "content");
        String text = MessagePluginDataReader.requiredString(
                content,
                "text",
                "content.text must not be blank"
        );
        content.put("text", text);
        data.put("content", content);

        String replyToMid = MessagePluginDataReader.optionalSnowflake(data.get("reply_to_mid"), "reply_to_mid");
        Map<String, Object> replyTo = MessagePluginDataReader.optionalObject(data, "reply_to");
        Map<String, Object> quoteReply = MessagePluginDataReader.optionalObject(data, "quote_reply");
        if (replyToMid == null && quoteReply == null) {
            throw ProblemException.validationFailed("reply_to_mid or quote_reply is required");
        }
        if (replyToMid != null) {
            data.put("reply_to_mid", replyToMid);
        }
        if (replyTo != null) {
            String snapshotMid = MessagePluginDataReader.requiredSnowflake(replyTo.get("mid"), "reply_to.mid");
            if (replyToMid == null || !replyToMid.equals(snapshotMid)) {
                throw ProblemException.validationFailed("reply_to_mid must match reply_to.mid");
            }
            replyTo.put("mid", snapshotMid);
            replyTo.put("sender_name", MessagePluginDataReader.requiredString(
                    replyTo, "sender_name", "reply_to.sender_name must not be blank"
            ));
            replyTo.put("preview", MessagePluginDataReader.requiredString(
                    replyTo, "preview", "reply_to.preview must not be blank"
            ));
            replyTo.put("created_at", MessagePluginDataReader.requiredPositiveLong(
                    replyTo, "created_at", "reply_to.created_at must be greater than 0"
            ));
            Boolean unavailable = MessagePluginDataReader.optionalBoolean(replyTo, "unavailable");
            if (unavailable != null) {
                replyTo.put("unavailable", unavailable);
            }
            data.put("reply_to", replyTo);
        }
        if (quoteReply != null) {
            quoteReply.put("mid", MessagePluginDataReader.requiredSnowflake(quoteReply.get("mid"), "quote_reply.mid"));
            quoteReply.put("uid", MessagePluginDataReader.requiredSnowflake(quoteReply.get("uid"), "quote_reply.uid"));
            quoteReply.put("preview", MessagePluginDataReader.requiredString(
                    quoteReply, "preview", "quote_reply.preview must not be blank"
            ));
            data.put("quote_reply", quoteReply);
        }
        Map<String, Object> linkPreview = MessagePluginDataReader.optionalObject(data, "link_preview");
        if (linkPreview != null) {
            data.put("link_preview", linkPreview);
        }
        return new CanonicalData(data, text);
    }
}
