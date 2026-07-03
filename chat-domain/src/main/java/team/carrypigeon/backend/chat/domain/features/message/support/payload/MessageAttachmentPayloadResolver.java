package team.carrypigeon.backend.chat.domain.features.message.support.payload;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileShareKeyCodec;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessagePayloadResolver;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息附件载荷出站解析器。
 * 职责：在读取/下发阶段把附件消息载荷转换为对外稳定的 `share_key` 文件引用语义。
 * 边界：只处理出站展示所需字段，不修改已持久化的消息载荷。
 */
public class MessageAttachmentPayloadResolver implements MessagePayloadResolver {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Logger log = LogManager.getLogger(MessageAttachmentPayloadResolver.class);

    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;
    private final JsonProvider jsonProvider;

    public MessageAttachmentPayloadResolver(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            JsonProvider jsonProvider
    ) {
        this.objectStorageServiceProvider = objectStorageServiceProvider;
        this.jsonProvider = jsonProvider;
    }

    /**
     * 解析出站消息载荷。
     *
     * @param messageType 消息类型
     * @param payload 已持久化的 canonical payload
     * @return 对于 file / voice 返回按基准协议收口后的 JSON，其余情况返回原值
     */
    @Override
    public String resolve(String messageType, String payload) {
        if (!isAttachmentMessageType(messageType) || payload == null || payload.isBlank()) {
            return payload;
        }
        try {
            Map<String, Object> resolvedPayload = new LinkedHashMap<>(jsonProvider.fromJson(payload, MAP_TYPE));
            String shareKey = stringValue(resolvedPayload, "share_key");
            String objectKey = firstNonBlank(
                    stringValue(resolvedPayload, "object_key"),
                    stringValue(resolvedPayload, "objectKey"),
                    shareKey == null ? null : FileShareKeyCodec.attachmentObjectKey(shareKey).orElse(null)
            );
            if (objectKey == null && shareKey != null) {
                resolvedPayload.put("download_path", FileShareKeyCodec.downloadPath(shareKey));
                resolvedPayload.remove("access_url");
                resolvedPayload.remove("access_url_expires_at");
                return jsonProvider.toJson(resolvedPayload);
            }
            if (objectKey == null) {
                return payload;
            }
            resolvedPayload.remove("object_key");
            resolvedPayload.remove("objectKey");
            resolvedPayload.remove("access_url");
            resolvedPayload.remove("access_url_expires_at");
            shareKey = FileShareKeyCodec.shareKeyForObjectKey(objectKey);
            resolvedPayload.put("share_key", shareKey);
            resolvedPayload.put("download_path", FileShareKeyCodec.downloadPath(shareKey));
            return jsonProvider.toJson(resolvedPayload);
        } catch (RuntimeException exception) {
            return payload;
        }
    }

    private boolean isAttachmentMessageType(String messageType) {
        return "file".equals(messageType) || "voice".equals(messageType);
    }

    void logPresignFallback(String objectKey, RuntimeException exception) {
        log.warn("Failed to create attachment presigned url, falling back to canonical payload. objectKey={}", objectKey, exception);
    }

    private String stringValue(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
