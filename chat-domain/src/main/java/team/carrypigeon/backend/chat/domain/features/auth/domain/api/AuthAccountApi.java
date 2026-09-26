package team.carrypigeon.backend.chat.domain.features.auth.domain.api;

import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.UpdateCurrentAccountEmailCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;

/**
 * 鉴权账号领域 API。
 * 职责：暴露账号注册与账号资料维护能力。
 * 边界：不暴露会话刷新、注销和 token 签发细节。
 * 输入：账号注册与账号资料维护命令对象。
 * 输出：注册结果投影或账号资料维护结果。
 * 失败语义：参数校验和账号唯一性等问题由领域问题异常表达。
 * 调用方：controller 或其它 feature 只能依赖本接口，不直接依赖具体实现类。
 */
public interface AuthAccountApi {

    /**
     * 注册新账号并初始化账号关联资料。
     * 输入：注册命令包含用户名和原始密码。
     * 输出：注册成功后的账号标识、用户名和初始化结果投影。
     * 约束：用户名必须满足账号唯一性规则，密码哈希和关联资料初始化由领域实现负责。
     * 副作用：创建用户信息表的原始数据。
     * @param command 账号注册业务命令
     * @return 注册完成后的账号结果投影
     */
    RegisterResult register(RegisterCommand command);

    /**
     * 查询账号当前邮箱登录标识。
     *
     * @param accountId 账号 ID
     * @return 当前邮箱登录标识
     */
    String getAccountEmail(long accountId);

    /**
     * 使用邮箱验证码更新当前账号的邮箱登录标识。
     * 输入：命令包含当前账号、新邮箱和验证码。
     * 副作用：消费有效验证码并持久化唯一的新邮箱。
     *
     * @param command 当前账号邮箱更新命令
     */
    void updateCurrentAccountEmail(UpdateCurrentAccountEmailCommand command);

}
