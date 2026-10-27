package team.carrypigeon.backend.chat.domain.features.auth.domain.api.token;

import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AuthTokenResult;

/**
 * 鉴权会话领域 API。
 * 职责：暴露登录、验证码会话、刷新与注销等会话生命周期能力。
 */
public interface AuthTokenApi {

    /**
     * 使用账号凭证登录并创建认证会话。
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功后的认证令牌结果
     */
    AuthTokenResult loginByPWD(String username,String password);

    /**
     * 使用邮箱验证吗登陆
     *
     * @param email 邮箱
     * @param code 验证码
     * @return 登录成功后的认证令牌结果
     */
    AuthTokenResult loginByEmail(String email,String code);

    /**
     * 校验 access token 并返回认证主体投影。
     * 失败语义：无法认证时抛出稳定的鉴权领域问题。
     *
     * @param accessToken access token
     * @return 已认证主体投影
     */
    AccessTokenAuthenticationResult authenticate(String accessToken);

    /**
     * 使用 refresh token 刷新认证会话。
     *
     * @param refreshToken 刷新的令牌
     * @return 刷新后的会话令牌结果
     */
    AuthTokenResult refreshToken(String refreshToken);

    /**
     * 注销当前认证会话。
     *
     * @param refreshToken 刷新令牌
     */
    void logout(String refreshToken);
}
