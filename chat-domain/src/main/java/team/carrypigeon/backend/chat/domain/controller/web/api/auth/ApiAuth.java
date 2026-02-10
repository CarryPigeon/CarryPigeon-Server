package team.carrypigeon.backend.chat.domain.controller.web.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;

/**
 * API 认证上下文工具类。
 * <p>
 * 提供从请求属性中读取当前认证用户 ID 的统一入口。
 */
public final class ApiAuth {

    /**
     * 请求属性中的用户 ID 键。
     */
    public static final String REQ_ATTR_UID = "cp_api_uid";

    /**
     * 工具类禁止实例化。
     */
    private ApiAuth() {
    }

    /**
     * 从请求上下文读取当前认证用户 ID。
     *
     * @param request HTTP 请求对象。
     * @return 当前认证用户 ID。
     * @throws CPProblemException 当请求未完成认证时抛出。
     */
    public static long requireUid(HttpServletRequest request) {
        Object v = request.getAttribute(REQ_ATTR_UID);
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                throw new CPProblemException(CPProblem.of(CPProblemReason.UNAUTHORIZED, "missing or invalid access token"));
            }
        }
        throw new CPProblemException(CPProblem.of(CPProblemReason.UNAUTHORIZED, "missing or invalid access token"));
    }
}
