package team.carrypigeon.backend.chat.domain.features.auth.domain.api;

import team.carrypigeon.backend.chat.domain.features.auth.domain.command.CreateTokenSessionCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;

/**
 * 鉴权会话领域 API。
 * 职责：暴露登录、验证码会话、刷新与注销等会话生命周期能力。
 * 边界：不暴露账号注册和验证码发送入口。
 * 输入：登录、刷新、注销与验证码会话命令对象。
 * 输出：访问令牌、刷新令牌和会话过期信息等认证投影。
 * 失败语义：认证失败、令牌无效、会话过期等问题由领域问题异常表达。
 * 调用方：controller 或 realtime 认证入口只能依赖本接口，不直接依赖具体实现类。
 */
public interface AuthSessionApi {

    /**
     * 使用验证码流程创建认证会话。
     * 输入：命令携带账号标识或验证码校验后生成会话所需资料。
     * 输出：包含 access token、refresh token 和会话过期时间的会话令牌投影。
     * 约束：调用方只消费令牌结果，不感知 token 签发与哈希实现。
     *
     * @param command 创建验证码会话的业务命令
     * @return 新建认证会话的令牌结果
     */
    AuthSessionTokenResult createTokenSession(CreateTokenSessionCommand command);

    /**
     * 使用账号凭证登录并创建认证会话。
     * 输入：登录命令包含登录标识和原始凭证。
     * 输出：登录成功后的 access token 与 refresh token 信息。
     * 失败语义：账号不存在、凭证错误或账号状态不允许登录时返回认证领域问题。
     *
     * @param command 账号登录业务命令
     * @return 登录成功后的认证令牌结果
     */
    AuthTokenResult login(LoginCommand command);

    /**
     * 使用 refresh token 刷新认证会话。
     * 输入：刷新命令包含当前 refresh token 与调用方上下文。
     * 输出：刷新后的 access token、refresh token 和会话过期时间。
     * 约束：旧 refresh token 的失效、轮换和重放判断由领域实现负责。
     *
     * @param command 刷新认证会话的业务命令
     * @return 刷新后的会话令牌结果
     */
    AuthSessionTokenResult refreshTokenSession(RefreshTokenCommand command);

    /**
     * 注销当前认证会话。
     * 输入：注销命令包含账号标识和 refresh token 或会话定位信息。
     * 副作用：使目标会话失效，后续刷新应被拒绝。
     * 约束：注销不暴露会话存储和 token 哈希细节。
     *
     * @param command 注销认证会话的业务命令
     */
    void logout(LogoutCommand command);
}
