package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UserIdRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UserMePatchRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UsersBatchRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

import java.util.Arrays;
import java.util.List;

/**
 * 用户 API 控制器。
 * <p>
 * 提供当前用户信息查询、资料更新、单用户查询与批量用户查询路由。
 */
@RestController
public class ApiUserController {

    private static final String CHAIN_USERS_ME = "api_users_me";
    private static final String CHAIN_USERS_ME_PATCH = "api_users_me_patch";
    private static final String CHAIN_USERS_GET = "api_users_get";
    private static final String CHAIN_USERS_BATCH = "api_users_batch";

    private final ApiFlowRunner flowRunner;

    /**
     * 构造用户控制器。
     *
     * @param flowRunner API 责任链执行器。
     */
    public ApiUserController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @param request HTTP 请求对象。
     * @return 标准用户资料响应。
     */
    @GetMapping("/api/users/me")
    public Object me(HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_USERS_ME, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param body 资料更新请求体。
     * @param request HTTP 请求对象。
     * @return 更新后的标准响应。
     */
    @PatchMapping("/api/users/me")
    public Object patchMe(@Valid @RequestBody UserMePatchRequest body, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, body);
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_USERS_ME_PATCH, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 按用户 ID 查询公开资料。
     *
     * @param uid 用户 ID。
     * @param request HTTP 请求对象。
     * @return 标准用户资料响应。
     */
    @GetMapping("/api/users/{uid}")
    public Object get(@PathVariable("uid") String uid, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new UserIdRequest(uid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_USERS_GET, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 按 ID 列表批量查询用户公开资料。
     *
     * @param ids 逗号分隔的用户 ID 字符串。
     * @param request HTTP 请求对象。
     * @return 批量查询标准响应。
     */
    @GetMapping(value = "/api/users", params = "ids")
    public Object batch(@RequestParam("ids") String ids, HttpServletRequest request) {
        List<String> idList = ids == null || ids.isBlank()
                ? List.of()
                : Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new UsersBatchRequest(idList));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_USERS_BATCH, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }
}
