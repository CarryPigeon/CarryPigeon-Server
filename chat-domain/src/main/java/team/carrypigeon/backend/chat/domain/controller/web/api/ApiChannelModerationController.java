package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.*;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * Channel moderation endpoints: applications and bans.
 */
@RestController
public class ApiChannelModerationController {

    private static final String CHAIN_APPLICATIONS_CREATE = "api_channel_applications_create";
    private static final String CHAIN_APPLICATIONS_LIST = "api_channel_applications_list";
    private static final String CHAIN_APPLICATION_DECISION = "api_channel_application_decision";
    private static final String CHAIN_BANS_PUT = "api_channel_bans_put";
    private static final String CHAIN_BANS_DELETE = "api_channel_bans_delete";
    private static final String CHAIN_BANS_LIST = "api_channel_bans_list";

    private final ApiFlowRunner flowRunner;

    public ApiChannelModerationController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * Route: {@code POST /api/channels/{cid}/applications}
     */
    @PostMapping("/api/channels/{cid}/applications")
    public ResponseEntity<Void> createApplication(@PathVariable("cid") String cid,
                                                  @Valid @RequestBody ChannelApplicationCreateRequest body,
                                                  HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelApplicationCreateInternalRequest(cid, body));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_APPLICATIONS_CREATE, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * Route: {@code GET /api/channels/{cid}/applications}
     */
    @GetMapping("/api/channels/{cid}/applications")
    public Object listApplications(@PathVariable("cid") String cid, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelApplicationListRequest(cid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_APPLICATIONS_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Route: {@code POST /api/channels/{cid}/applications/{application_id}/decisions}
     */
    @PostMapping("/api/channels/{cid}/applications/{application_id}/decisions")
    public ResponseEntity<Void> decide(@PathVariable("cid") String cid,
                                       @PathVariable("application_id") String applicationId,
                                       @Valid @RequestBody ChannelApplicationDecisionRequest body,
                                       HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelApplicationDecisionInternalRequest(cid, applicationId, body));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_APPLICATION_DECISION, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * Route: {@code PUT /api/channels/{cid}/bans/{uid}}
     */
    @PutMapping("/api/channels/{cid}/bans/{uid}")
    public ResponseEntity<Void> banPut(@PathVariable("cid") String cid,
                                       @PathVariable("uid") String uid,
                                       @Valid @RequestBody ChannelBanUpsertRequest body,
                                       HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelBanTargetRequest(cid, uid, body));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_BANS_PUT, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * Route: {@code DELETE /api/channels/{cid}/bans/{uid}}
     */
    @DeleteMapping("/api/channels/{cid}/bans/{uid}")
    public ResponseEntity<Void> banDelete(@PathVariable("cid") String cid,
                                          @PathVariable("uid") String uid,
                                          HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelMemberTargetRequest(cid, uid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_BANS_DELETE, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * Route: {@code GET /api/channels/{cid}/bans}
     */
    @GetMapping("/api/channels/{cid}/bans")
    public Object bans(@PathVariable("cid") String cid, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, new ChannelIdRequest(cid));
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_BANS_LIST, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }
}

