package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelIdRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelMemberTargetRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * Channel membership and admin management endpoints.
 */
@RestController
public class ApiChannelMemberController {

    private static final String CHAIN_MEMBERS_LIST = "api_channel_members_list";
    private static final String CHAIN_MEMBERS_KICK = "api_channel_members_kick";
    private static final String CHAIN_ADMINS_PUT = "api_channel_admins_put";
    private static final String CHAIN_ADMINS_DELETE = "api_channel_admins_delete";

    private final ApiFlowRunner flowRunner;

    public ApiChannelMemberController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * Route: {@code GET /api/channels/{cid}/members}
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
     * Route: {@code DELETE /api/channels/{cid}/members/{uid}}
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
     * Route: {@code PUT /api/channels/{cid}/admins/{uid}}
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
     * Route: {@code DELETE /api/channels/{cid}/admins/{uid}}
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

