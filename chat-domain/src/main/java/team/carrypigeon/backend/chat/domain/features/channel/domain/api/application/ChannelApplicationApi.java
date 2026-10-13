package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.ListChannelApplicationsQuery;
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
     * 输入：命令携带申请账号、目标频道和申请说明。
     * 输出：新建申请的领域投影。
     * 约束：公开频道、重复申请和已有成员关系由领域规则处理。
     *
     * @param command 频道加入申请创建业务命令
     * @return 创建后的频道申请投影
     */
    ChannelApplicationResult createChannelApplication(CreateChannelApplicationCommand command);

    /**
     * 查询频道加入申请列表。
     * 输入：查询对象携带操作者、频道和筛选条件。
     * 输出：符合条件的频道申请投影列表。
     * 约束：只有具备治理或审批权限的账号可以读取受限申请列表。
     *
     * @param query 频道申请列表查询条件
     * @return 频道申请投影列表
     */
    List<ChannelApplicationResult> listChannelApplications(ListChannelApplicationsQuery query);

    /**
     * 审批频道加入申请。
     * 输入：命令携带审批操作者、申请标识和审批决定。
     * 输出：审批后的频道申请投影。
     * 副作用：通过申请时会创建或恢复频道成员关系。
     *
     * @param command 频道申请审批业务命令
     * @return 审批后的频道申请投影
     */
    ChannelApplicationResult decideChannelApplication(DecideChannelApplicationCommand command);
}
