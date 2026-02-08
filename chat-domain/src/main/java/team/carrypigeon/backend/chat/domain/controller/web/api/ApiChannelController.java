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
 * Channel resources under {@code /api/channels}.
 * <p>
 * All endpoints require {@code Authorization: Bearer <access_token>}.
 * Business logic is implemented in LiteFlow chains (see {@code application-starter/src/main/resources/config/api.xml}).
 */
@RestController
public class ApiChannelController {

    private static final String CHAIN_CHANNELS_CREATE = "api_channels_create";
    private static final String CHAIN_CHANNELS_GET = "api_channels_get";
    private static final String CHAIN_CHANNELS_PATCH = "api_channels_patch";
    private static final String CHAIN_CHANNELS_DELETE = "api_channels_delete";

    private final ApiFlowRunner flowRunner;

    public ApiChannelController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * Create a channel.
     * <p>
     * Route: {@code POST /api/channels}
     * <p>
     * Chain: {@code api_channels_create}
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
     * Get channel profile.
     * <p>
     * Route: {@code GET /api/channels/{cid}}
     * <p>
     * Chain: {@code api_channels_get}
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
     * Update channel profile.
     * <p>
     * Route: {@code PATCH /api/channels/{cid}}
     * <p>
     * Chain: {@code api_channels_patch}
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
     * Delete a channel.
     * <p>
     * Route: {@code DELETE /api/channels/{cid}}
     * <p>
     * Chain: {@code api_channels_delete}
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

