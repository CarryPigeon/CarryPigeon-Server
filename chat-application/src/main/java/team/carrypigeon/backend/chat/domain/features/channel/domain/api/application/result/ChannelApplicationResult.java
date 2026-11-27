package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;

/**
 * 入群申请结果投影。
 * 职责：向协议层暴露单条入群申请的申请人与审批状态字段。
 * 边界：只承载展示字段，不承载申请状态流转与审批权限判断；此处的账户字段表示申请人而非审批人。
 *
 * @param applicationId 申请记录 ID
 * @param channelId 申请加入的频道 ID
 * @param accountId 申请人账户 ID
 * @param reason 申请理由；为空时统一归一为空字符串
 * @param applyTime 申请创建时间
 * @param status 申请状态名，大写形式
 */
public record ChannelApplicationResult(
    long applicationId,
    long channelId,
    long accountId,
    String reason,
    Instant applyTime,
    String status
) {

    public static ChannelApplicationResult from(ChannelApplication application, String reason) {
        return new ChannelApplicationResult(
                application.applicationId(),
                application.channelId(),
                application.applicantId(),
                reason == null ? application.reason() : reason,
                application.createdAt(),
                application.status().name()
        );
    }
}
