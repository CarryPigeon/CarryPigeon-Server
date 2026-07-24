package team.carrypigeon.backend.chat.domain.features.auth.domain.api;

import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;

/**
 * Access token 认证领域 API。
 * 职责：向 HTTP 与 WebSocket 协议层暴露 access token 认证结果。
 * 边界：不暴露 JWT claims、签名算法或 refresh token 能力。
 * 输入：本服务端签发的 access token 字符串。
 * 输出：包含账号 ID、账号名和过期时间的稳定认证投影。
 * 失败语义：token 无效、类型错误、过期或主体非法时由领域问题异常表达。
 * 调用方：HTTP 与 WebSocket 认证入口只能依赖本接口，不直接依赖 token codec。
 */
public interface AccessTokenAuthenticationApi {

    /**
     * 校验 access token 并返回认证主体投影。
     * 失败语义：无法认证时抛出稳定的鉴权领域问题。
     *
     * @param accessToken access token
     * @return 已认证主体投影
     */
    AccessTokenAuthenticationResult authenticate(String accessToken);
}
