package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 文件频道消息插件。
 * 职责：校验文件引用并生成文件消息的正文、预览、检索与持久化载荷。
 * 边界：只依赖 storage-api 抽象，不接入具体对象存储实现。
 */
public class FileChannelMessagePlugin implements ChannelMessagePlugin {

    private final ObjectStorageService objectStorageService;
    private final JsonProvider jsonProvider;
    private final MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy;

    public FileChannelMessagePlugin(
            ObjectStorageService objectStorageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy
    ) {
        this.objectStorageService = objectStorageService;
        this.jsonProvider = jsonProvider;
        this.messageAttachmentObjectKeyPolicy = messageAttachmentObjectKeyPolicy;
    }

    /**
     * 返回当前插件负责的消息类型。
     *
     * @return `file` 消息类型标识
     */
    @Override
    public String supportedType() {
        return "file";
    }

    /**
     * 校验文件草稿并构造领域文件消息。
     * 输入：消息构建上下文与文件草稿。
     * 约束：对象键必须属于当前发送者允许的频道附件作用域。
     * 输出：包含文件载荷与检索文本的领域消息对象。
     *
     * @param context 消息构建上下文
     * @param draft 入站消息草稿
     * @return 文件领域消息
     */
    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof FileChannelMessageDraft fileDraft)) {
            throw new IllegalArgumentException("file plugin only supports FileChannelMessageDraft");
        }
        requireScopedObjectKey(context, fileDraft.objectKey());
        StorageObject storageObject = requireStorageObject(fileDraft.objectKey());
        String filename = requireNonBlank(fileDraft.filename(), "filename must not be blank");
        String resolvedMimeType = firstNonBlank(fileDraft.mimeType(), storageObject.contentType());
        long resolvedSize = fileDraft.size() != null && fileDraft.size() > 0 ? fileDraft.size() : storageObject.size();
        String normalizedBody = normalizeBody(fileDraft.body(), filename);
        String previewText = "[文件消息] " + filename;
        String searchableText = joinSearchableText(normalizedBody, filename);

        return new ChannelMessage(
                context.messageId(),
                context.serverId(),
                context.conversationId(),
                context.channelId(),
                context.senderId(),
                supportedType(),
                normalizedBody,
                previewText,
                searchableText,
                jsonProvider.toJson(new FileMessagePayload(fileDraft.objectKey(), filename, resolvedMimeType, resolvedSize)),
                fileDraft.metadata(),
                "sent",
                context.createdAt()
        );
    }

    private StorageObject requireStorageObject(String objectKey) {
        return objectStorageService.get(new GetObjectCommand(objectKey))
                .orElseThrow(() -> ProblemException.notFound("storage object does not exist"));
    }

    private void requireScopedObjectKey(ChannelMessageBuildContext context, String objectKey) {
        if (!messageAttachmentObjectKeyPolicy.isWithinSenderScope(
                context.channelId(),
                supportedType(),
                context.senderId(),
                objectKey
        )) {
            throw ProblemException.validationFailed("file objectKey is out of allowed channel scope");
        }
    }

    private String normalizeBody(String body, String fallback) {
        return body == null || body.isBlank() ? fallback : body.trim();
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed(message);
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "application/octet-stream" : second;
    }

    private String joinSearchableText(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private record FileMessagePayload(String objectKey, String filename, String mimeType, long size) {
    }
}
