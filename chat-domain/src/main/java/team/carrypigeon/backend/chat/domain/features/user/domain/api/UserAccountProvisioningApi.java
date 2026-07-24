package team.carrypigeon.backend.chat.domain.features.user.domain.api;

import team.carrypigeon.backend.chat.domain.features.user.domain.command.InitializeUserAccountProfileCommand;

/**
 * 用户账号资料初始化 API。
 * 职责：为新建账号创建所属 user feature 的初始公开资料。
 * 边界：不创建鉴权账号，不处理频道成员关系。
 * 输入：包含新账号 ID、初始昵称与创建时间的资料初始化命令。
 * 输出：无返回值，副作用为保存初始用户资料。
 * 失败语义：资料冲突或保存失败时由领域问题异常表达。
 * 调用方：auth 注册编排只能依赖本接口，不直接访问 user 仓储或模型。
 */
public interface UserAccountProvisioningApi {

    /**
     * 初始化新账号的用户资料。
     * 副作用：保存账号对应的初始公开资料。
     *
     * @param command 用户资料初始化命令
     */
    void initializeProfile(InitializeUserAccountProfileCommand command);
}
