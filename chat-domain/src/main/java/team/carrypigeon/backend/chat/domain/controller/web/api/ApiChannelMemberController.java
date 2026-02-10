package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelIdRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelMemberTargetRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * 频道成员 API 控制器。
 * <p>
 * 提供成员列表查询、移除成员与管理员授予/撤销路由。
 */
@RestController
public class ApiChannelMemberController {

    private static final String CHAIN_MEMBERS_LIST = "api_channel_members_list";
    private static final String CHAIN_MEMBERS_KICK = "api_channel_members_kick";
    private static final String CHAIN_ADMINS_PUT = "api_channel_admins_put";
    private static final String CHAIN_ADMINS_DELETE = "api_channel_admins_delete";

    private final ApiFlowRunner flowRunner;

    /**
     * 构造频道成员控制器。
     *
     * @param flowRunner API 责任链执行器。
     */
    public ApiChannelMemberController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * 查询频道成员列表。
     *
     * @param cid 频道 ID。
     * @param request HTTP 请求对象。
     * @return 标准成员列表响应。
     */
    @GetMapping("/api/channels/{cid}/members")
    public Object list(@PathVariable("cid") String cid, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelIdRequest(cid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_MEMBERS_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 移除频道成员。
     *
     * @param cid 频道 ID。
     * @param uid 目标用户 ID。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
     */
    @DeleteMapping("/api/channels/{cid}/members/{uid}")
    public ResponseEntity<Void> kick(@PathVariable("cid") String cid,
                                     @PathVariable("uid") String uid,
                                     HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelMemberTargetRequest(cid, uid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_MEMBERS_KICK, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * 授予频道管理员权限。
     *
     * @param cid 频道 ID。
     * @param uid 目标用户 ID。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
     */
    @PutMapping("/api/channels/{cid}/admins/{uid}")
    public ResponseEntity<Void> adminPut(@PathVariable("cid") String cid,
                                         @PathVariable("uid") String uid,
                                         HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelMemberTargetRequest(cid, uid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_ADMINS_PUT, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * 撤销频道管理员权限。
     *
     * @param cid 频道 ID。
     * @param uid 目标用户 ID。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
     */
    @DeleteMapping("/api/channels/{cid}/admins/{uid}")
    public ResponseEntity<Void> adminDelete(@PathVariable("cid") String cid,
                                            @PathVariable("uid") String uid,
                                            HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelMemberTargetRequest(cid, uid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_ADMINS_DELETE, ctx);
        return ResponseEntity.noContent().build();
    }
}
