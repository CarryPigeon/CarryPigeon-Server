package team.carrypigeon.backend.chat.domain.controller.web.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;

/**
 * Small helper for extracting authentication info from the current HTTP request.
 * <p>
 * {@link ApiAccessTokenFilter} writes the authenticated user id into request attribute {@link #REQ_ATTR_UID}.
 */
public final class ApiAuth {

    public static final String REQ_ATTR_UID = "cp_api_uid";

    private ApiAuth() {
    }

    public static long requireUid(HttpServletRequest request) {
        Object v = request.getAttribute(REQ_ATTR_UID);
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof String s) {
            return Long.parseLong(s);
        }
        throw new CPProblemException(CPProblem.of(401, "unauthorized", "missing or invalid access token"));
    }
}
