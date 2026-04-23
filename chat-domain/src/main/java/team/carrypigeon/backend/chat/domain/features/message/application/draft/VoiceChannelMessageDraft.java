package team.carrypigeon.backend.chat.domain.features.message.application.draft;

/**
 * 语音频道消息草稿。
 * 职责：表达语音消息在持久化前的最小输入。
 * 边界：这里只承载语音引用和必要元信息，不直接承载对象存储实现细节。
 *
 * @param body 语音消息正文，可为空，留给插件回退为转写或预览文本
 * @param objectKey 对象存储键
 * @param filename 文件名
 * @param mimeType 语音 MIME 类型，可为空，留给插件回退
 * @param size 语音大小，可为空或非正数，留给插件回退
 * @param durationMillis 语音时长（毫秒）
 * @param transcript 转写文本，可为空
 * @param metadata 原始元数据 JSON，可为空
 */
public record VoiceChannelMessageDraft(
        String body,
        String objectKey,
        String filename,
        String mimeType,
        Long size,
        Long durationMillis,
        String transcript,
        String metadata
) implements ChannelMessageDraft {

    @Override
    public String type() {
        return "voice";
    }

    @Override
    public String payload() {
        return null;
    }
}
