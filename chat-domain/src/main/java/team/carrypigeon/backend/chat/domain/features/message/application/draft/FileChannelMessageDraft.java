package team.carrypigeon.backend.chat.domain.features.message.application.draft;

/**
 * 文件频道消息草稿。
 * 职责：表达文件消息在持久化前的最小输入。
 * 边界：这里只承载文件引用和必要元信息，不直接承载对象存储实现细节。
 *
 * @param body 文件消息正文，可为空，留给插件回退为文件名
 * @param objectKey 对象存储键
 * @param filename 文件名
 * @param mimeType 文件 MIME 类型，可为空，留给插件回退
 * @param size 文件大小，可为空或非正数，留给插件回退
 * @param metadata 原始元数据 JSON，可为空
 */
public record FileChannelMessageDraft(
        String body,
        String objectKey,
        String filename,
        String mimeType,
        Long size,
        String metadata
) implements ChannelMessageDraft {

    @Override
    public String type() {
        return "file";
    }

    @Override
    public String payload() {
        return null;
    }
}
