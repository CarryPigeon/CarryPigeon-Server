package team.carrypigeon.backend.chat.domain.features.message.application.draft;

/**
 * 通用频道消息草稿。
 * 职责：表达消息在持久化前、由插件负责解释的最小输入结构。
 * 边界：这里只表达插件分派所需的稳定字段，不直接承载协议层细节。
 */
public sealed interface ChannelMessageDraft permits TextChannelMessageDraft, FileChannelMessageDraft, VoiceChannelMessageDraft, PluginChannelMessageDraft, CustomChannelMessageDraft, SystemChannelMessageDraft {

    /**
     * 返回当前草稿所属的稳定消息类型。
     *
     * @return 消息类型标识
     */
    String type();

    /**
     * 返回当前草稿的兼容内容字段。
     *
     * @return 内容字段，必要时可为空字符串但不应为 null
     */
    String body();

    /**
     * 返回当前草稿的原始结构化载荷。
     *
     * @return 结构化载荷 JSON，可为空；若由具体插件负责生成最终载荷，可返回 null
     */
    String payload();

    /**
     * 返回当前草稿的元数据。
     *
     * @return 元数据 JSON，可为空
     */
    String metadata();
}
