package team.carrypigeon.backend.chat.domain.features.message.support.payload;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息附件载荷出站解析器。
 * 职责：在读取/下发阶段为 file / voice 消息派生临时访问 URL，同时保留 canonical objectKey。
 * 边界：只处理出站展示所需字段，不修改已持久化的消息载荷。
 */
public class MessageAttachmentPayloadResolver {

    private static final Duration ACCESS_URL_TTL = Duration.ofMinutes(30);
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
     * @return 对于 file / voice 返回附加访问字段后的 JSON，其余情况返回原值
     */
    public String resolve(String messageType, String payload) {
        if (!isAttachmentMessageType(messageType) || payload == null || payload.isBlank()) {
            return payload;
        }
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            return payload;
        }
        try {
            Map<String, Object> resolvedPayload = new LinkedHashMap<>(jsonProvider.fromJson(payload, MAP_TYPE));
            Object objectKeyValue = resolvedPayload.get("object_key");
            if (!(objectKeyValue instanceof String objectKey) || objectKey.isBlank()) {
                return payload;
            }
            PresignedUrl presignedUrl;
            try {
                presignedUrl = objectStorageService.createPresignedUrl(
                        new PresignedUrlCommand(objectKey, ACCESS_URL_TTL)
                );
            } catch (RuntimeException exception) {
                logPresignFallback(objectKey, exception);
                return payload;
            }
            resolvedPayload.put("access_url", presignedUrl.url().toString());
            resolvedPayload.put("access_url_expires_at", presignedUrl.expiresAt());
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
}
