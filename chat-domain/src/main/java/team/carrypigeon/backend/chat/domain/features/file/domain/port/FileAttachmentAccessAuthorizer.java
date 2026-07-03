package team.carrypigeon.backend.chat.domain.features.file.domain.port;

/**
 * 文件附件访问授权端口。
 * 职责：表达 file feature 读取消息附件前需要的访问判定。
 * 边界：只暴露 file 所需语义，不泄漏 channel 仓储或成员模型。
 */
public interface FileAttachmentAccessAuthorizer {

    /**
     * 判断账户是否可以读取指定频道内的消息附件。
     *
     * @param accountId 当前账户 ID
     * @param channelId 附件所属频道 ID
     * @return 允许读取时返回 true
     */
    boolean canAccessChannelAttachment(long accountId, long channelId);
}
