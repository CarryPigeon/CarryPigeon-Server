package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 语音频道消息插件。
 * 职责：校验语音引用并生成语音消息的正文、预览、检索与持久化载荷。
 * 边界：只依赖 storage-api 抽象，不接入具体对象存储实现。
 */
public class VoiceChannelMessagePlugin implements ChannelMessagePlugin {

    private final ObjectStorageService objectStorageService;
    private final JsonProvider jsonProvider;
    private final MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy;

    public VoiceChannelMessagePlugin(
            ObjectStorageService objectStorageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy
    ) {
        this.objectStorageService = objectStorageService;
        this.jsonProvider = jsonProvider;
        this.messageAttachmentObjectKeyPolicy = messageAttachmentObjectKeyPolicy;
    }

    @Override
    public String supportedType() {
        return "voice";
    }

    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof VoiceChannelMessageDraft voiceDraft)) {
            throw new IllegalArgumentException("voice plugin only supports VoiceChannelMessageDraft");
        }
        requireScopedObjectKey(context, voiceDraft.objectKey());
        StorageObject storageObject = objectStorageService.get(new GetObjectCommand(voiceDraft.objectKey()))
                .orElseThrow(() -> ProblemException.notFound("storage object does not exist"));
        String filename = requireNonBlank(voiceDraft.filename(), "filename must not be blank");
        long durationMillis = voiceDraft.durationMillis() == null ? 0L : voiceDraft.durationMillis();
        if (durationMillis <= 0L) {
            throw ProblemException.validationFailed("durationMillis must be greater than 0");
        }
        String resolvedMimeType = firstNonBlank(voiceDraft.mimeType(), storageObject.contentType());
        long resolvedSize = voiceDraft.size() != null && voiceDraft.size() > 0 ? voiceDraft.size() : storageObject.size();
        String normalizedBody = normalizeBody(voiceDraft.body(), voiceDraft.transcript(), buildPreview(filename, durationMillis));
        String previewText = buildPreview(filename, durationMillis);
        String searchableText = joinSearchableText(normalizedBody, voiceDraft.transcript(), filename);

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
                jsonProvider.toJson(new VoiceMessagePayload(
                        voiceDraft.objectKey(),
                        filename,
                        resolvedMimeType,
                        resolvedSize,
                        durationMillis,
                        voiceDraft.transcript()
                )),
                voiceDraft.metadata(),
                "sent",
                context.createdAt()
        );
    }

    private String buildPreview(String filename, long durationMillis) {
        return "[语音消息] " + filename + " " + Math.max(1, durationMillis / 1000) + "s";
    }

    private void requireScopedObjectKey(ChannelMessageBuildContext context, String objectKey) {
        if (!messageAttachmentObjectKeyPolicy.isWithinSenderScope(
                context.channelId(),
                supportedType(),
                context.senderId(),
                objectKey
        )) {
            throw ProblemException.validationFailed("voice objectKey is out of allowed channel scope");
        }
    }

    private String normalizeBody(String body, String transcript, String fallback) {
        if (body != null && !body.isBlank()) {
            return body.trim();
        }
        if (transcript != null && !transcript.isBlank()) {
            return transcript.trim();
        }
        return fallback;
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
        return second == null ? "audio/mpeg" : second;
    }

    private String joinSearchableText(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private record VoiceMessagePayload(
            String objectKey,
            String filename,
            String mimeType,
            long size,
            long durationMillis,
            String transcript
    ) {
    }
}
