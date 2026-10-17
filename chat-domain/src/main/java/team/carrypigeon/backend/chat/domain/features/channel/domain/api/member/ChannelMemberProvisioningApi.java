package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.input.InitializeChannelMembershipsCommand;

/**
 * 新账号频道成员关系初始化 API。
 * 职责：为新建账号建立全部系统频道的成员关系，使其登录后即可看到默认频道。
 * 边界：不创建频道本身，不创建鉴权账号与用户资料，也不处理普通频道的加入流程。
 * 输入：包含新账号 ID 与加入时间的初始化命令。
 * 输出：无返回值，成功即表示成员关系已补齐。
 * 失败语义：本实现按幂等语义跳过已存在的成员关系；系统频道不存在时不写入也不抛异常。
 * 调用方：auth feature 的账号开通协作对象在账号创建事务内调用。
 */
public interface ChannelMemberProvisioningApi {

    /**
     * 初始化新账号的基础频道成员关系。
     * 副作用：为账号保存其尚未加入的各个系统频道的成员关系，角色固定为 MEMBER。
     * 约束：幂等，重复调用不会产生重复成员关系。
     *
     * @param command 成员关系初始化命令
     */
    void initializeMemberships(InitializeChannelMembershipsCommand command);
}
