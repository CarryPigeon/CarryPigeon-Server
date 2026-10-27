package team.carrypigeon.backend.chat.domain.features.auth.domain.api.account;

import java.util.List;

import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;

/**
 * 鉴权账号领域 API。
 * 职责：暴露账号注册与账号资料维护能力。
 * 边界：不暴露会话刷新、注销和 token 签发细节。
 */
public interface AuthAccountApi {

    /**
     * 注册新账号并初始化账号关联资料。
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param code 验证码
     * @return 注册完成后的账号结果投影
     */
    RegisterResult register(String username, String password,String email,String code);

    /**
     * 获取当前账户公开信息
     *
     * @param accountId 账号 ID
     * @return 当前账户公开信息
     */
    AuthAccount getAccount(long accountId);

    /**
     * 批量获取账户公开信息。
     *
     * @param accountIds 目标账户 ID 列表
     * @return 命中的账户公开信息
     */
    List<AuthAccount> getAccounts(List<Long> accountIds);

    /**
     * 使用邮箱验证码更新当前账号的邮箱登录标识。
     * 输入：命令包含当前账号、新邮箱和验证码。
     * 副作用：消费有效验证码并持久化唯一的新邮箱。
     *
     * @param accountId 当前账号id
     * @param email 新邮箱
     * @param code 邮箱验证码
     */
    void updateEmail(long accountId, String email, String code);

}
