package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result.ChannelApplicationResult;

/**
 * 频道申请流领域 API。
 * 职责：暴露邀请、接受邀请、入群申请和审批能力。
 * 边界：不暴露 controller 协议、具体实现类和持久化细节。
 * 输入：邀请、申请、审批命令与申请列表查询对象。
 * 输出：频道邀请或频道申请投影。
 * 失败语义：权限不足、重复申请、邀请失效和状态冲突由领域问题异常表达。
 * 调用方：controller 或其它 feature 通过本接口完成频道加入流程。
 */
public interface ChannelApplicationApi {

    /**
     * 创建加入频道申请。
     *
     * @param accountId 申请账号
     * @param channelId 目标频道
     * @param reason 申请说明
     * @return 创建后的频道申请投影
     */
    ChannelApplicationResult createChannelApplication(long accountId, long channelId, String reason);

    /**
     * 查询频道加入申请列表。
     *
     * @param accountId 操作者账号
     * @param channelId 频道
     * @return 频道申请投影列表
     */
    List<ChannelApplicationResult> listChannelApplications(long accountId, long channelId);

    /**
     * 审批频道加入申请。
     *
     * @param operatorAccountId 操作者账号
     * @param channelId 频道
     * @param applicationId 申请
     * @param decision 审批决定
     *
     * @return 审批后的频道申请投影
     */
    ChannelApplicationResult decideChannelApplication(long operatorAccountId, long channelId, long applicationId, String decision);
}
