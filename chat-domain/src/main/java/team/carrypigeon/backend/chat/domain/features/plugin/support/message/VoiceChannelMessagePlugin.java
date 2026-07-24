package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.util.LinkedHashMap;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * Core:Voice 消息插件。
 * 职责：校验语音附件、时长和可选转写，并生成 canonical data 和 preview。
 * 边界：只通过 file-api 与 storage-api 验证引用，不负责上传、权限或消息持久化。
 */
public class VoiceChannelMessagePlugin implements ChannelMessagePlugin {

    private final ObjectStorageService objectStorageService;
    private final FileReferenceApi fileReferenceApi;

    public VoiceChannelMessagePlugin(
            ObjectStorageService objectStorageService,
            FileReferenceApi fileReferenceApi
    ) {
        this.objectStorageService = objectStorageService;
        this.fileReferenceApi = fileReferenceApi;
    }

    @Override
    public String supportedType() {
        return "voice";
    }

    @Override
    public String supportedDomain() {
        return "Core:Voice";
    }

    @Override
    public CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> rawData
    ) {
        MessagePluginDataReader.requireVersion(domainVersion, "1.0.0");
        Map<String, Object> data = MessagePluginDataReader.copyData(rawData);
        String objectKey = MessagePluginDataReader.resolveAttachmentObjectKey(data, fileReferenceApi);
        requireScopedObjectKey(context, objectKey);
        StorageObject storageObject = objectStorageService.get(new GetObjectCommand(objectKey))
                .orElseThrow(() -> ProblemException.notFound("storage object does not exist"));
        String filename = MessagePluginDataReader.requiredString(data, "filename", "filename must not be blank");
        long durationMillis = MessagePluginDataReader.requiredPositiveLong(
                data,
                "duration_millis",
                "duration_millis must be greater than 0"
        );
        String mimeType = firstNonBlank(
                MessagePluginDataReader.optionalString(data, "mime_type"),
                storageObject.contentType(),
                "audio/mpeg"
        );
        Long requestedSize = MessagePluginDataReader.optionalLong(data, "size");
        long size = requestedSize != null && requestedSize > 0 ? requestedSize : storageObject.size();
        String shareKey = fileReferenceApi.shareKeyForObjectKey(objectKey);
        Map<String, Object> canonicalData = new LinkedHashMap<>();
        canonicalData.put("share_key", shareKey);
        canonicalData.put("download_path", fileReferenceApi.downloadPath(shareKey));
        canonicalData.put("filename", filename);
        canonicalData.put("mime_type", mimeType);
        canonicalData.put("size", size);
        canonicalData.put("duration_millis", durationMillis);
        String transcript = MessagePluginDataReader.optionalString(data, "transcript");
        if (transcript != null) {
            canonicalData.put("transcript", transcript);
        }
        String text = MessagePluginDataReader.optionalString(data, "text");
        if (text != null) {
            canonicalData.put("text", text);
        }
        String preview = "[语音消息] " + filename + " " + Math.max(1, durationMillis / 1000) + "s";
        return new CanonicalData(canonicalData, preview);
    }

    private void requireScopedObjectKey(ChannelMessageBuildContext context, String objectKey) {
        if (!fileReferenceApi.isMessageAttachmentWithinSenderScope(
                context.channelId(), supportedType(), context.senderId(), objectKey
        )) {
            throw ProblemException.validationFailed("voice objectKey is out of allowed channel scope");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
