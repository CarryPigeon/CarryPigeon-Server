package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelCreateRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelIdRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelPatchInternalRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelPatchRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * 频道基础 API 控制器。
 * <p>
 * 提供频道创建、查询、更新、删除等核心路由。
 */
@RestController
public class ApiChannelController {

    private static final String CHAIN_CHANNELS_CREATE = "api_channels_create";
    private static final String CHAIN_CHANNELS_GET = "api_channels_get";
    private static final String CHAIN_CHANNELS_PATCH = "api_channels_patch";
    private static final String CHAIN_CHANNELS_DELETE = "api_channels_delete";

    private final ApiFlowRunner flowRunner;

    /**
     * 创建频道控制器。
     *
     * @param flowRunner API 链路执行器
     */
    public ApiChannelController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * 创建频道。
     * <p>
     * Route: {@code POST /api/channels}
     * Chain: {@code api_channels_create}
     *
     * @param body 频道创建请求体
     * @param request HTTP 请求对象（用于获取鉴权用户）
     * @return HTTP 201 响应，响应体为标准协议对象
     */
    @PostMapping("/api/channels")
    public ResponseEntity<Object> create(@Valid @RequestBody ChannelCreateRequest body, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, body);
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_CHANNELS_CREATE, ctx);
        return ResponseEntity.status(201).body(ctx.get(CPFlowKeys.RESPONSE));
    }

    /**
     * 获取频道资料。
     * <p>
     * Route: {@code GET /api/channels/{cid}}
     * Chain: {@code api_channels_get}
     *
     * @param cid 频道 ID（字符串形式）
     * @param request HTTP 请求对象（用于获取鉴权用户）
     * @return 标准协议响应对象
     */
    @GetMapping("/api/channels/{cid}")
    public Object get(@PathVariable("cid") String cid, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelIdRequest(cid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_CHANNELS_GET, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 更新频道资料。
     * <p>
     * Route: {@code PATCH /api/channels/{cid}}
     * Chain: {@code api_channels_patch}
     *
     * @param cid 频道 ID（字符串形式）
     * @param body 频道更新请求体
     * @param request HTTP 请求对象（用于获取鉴权用户）
     * @return 标准协议响应对象
     */
    @PatchMapping("/api/channels/{cid}")
    public Object patch(@PathVariable("cid") String cid,
                        @RequestBody ChannelPatchRequest body,
                        HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelPatchInternalRequest(cid, body));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_CHANNELS_PATCH, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 删除频道。
     * <p>
     * Route: {@code DELETE /api/channels/{cid}}
     * Chain: {@code api_channels_delete}
     *
     * @param cid 频道 ID（字符串形式）
     * @param request HTTP 请求对象（用于获取鉴权用户）
     * @return 空内容响应（HTTP 204）
     */
    @DeleteMapping("/api/channels/{cid}")
    public ResponseEntity<Void> delete(@PathVariable("cid") String cid, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelIdRequest(cid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_CHANNELS_DELETE, ctx);
        return ResponseEntity.noContent().build();
    }
}
