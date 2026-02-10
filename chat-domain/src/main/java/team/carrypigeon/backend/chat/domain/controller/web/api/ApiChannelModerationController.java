package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationCreateInternalRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationCreateRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationDecisionInternalRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationDecisionRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationListRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelBanTargetRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelBanUpsertRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelIdRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelMemberTargetRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * 频道审核与禁言 API 控制器。
 * <p>
 * 提供入群申请创建/审批、禁言设置/解除、禁言列表查询路由。
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

    /**
     * 构造频道审核控制器。
     *
     * @param flowRunner API 责任链执行器。
     */
    public ApiChannelModerationController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * 创建入群申请。
     *
     * @param cid 频道 ID。
     * @param body 申请请求体。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
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
     * 查询频道入群申请列表。
     *
     * @param cid 频道 ID。
     * @param request HTTP 请求对象。
     * @return 标准申请列表响应。
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
     * 审批入群申请。
     *
     * @param cid 频道 ID。
     * @param applicationId 申请 ID。
     * @param body 审批请求体。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
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
     * 设置用户禁言。
     *
     * @param cid 频道 ID。
     * @param uid 目标用户 ID。
     * @param body 禁言配置请求体。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
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
     * 解除用户禁言。
     *
     * @param cid 频道 ID。
     * @param uid 目标用户 ID。
     * @param request HTTP 请求对象。
     * @return HTTP 204 无内容响应。
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
     * 查询频道禁言列表。
     *
     * @param cid 频道 ID。
     * @param request HTTP 请求对象。
     * @return 标准禁言列表响应。
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
