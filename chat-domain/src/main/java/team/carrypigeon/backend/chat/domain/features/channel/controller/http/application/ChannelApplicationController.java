package team.carrypigeon.backend.chat.domain.features.channel.controller.http.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.ChannelApplicationApi;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.ChannelApplicationListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.ChannelApplicationResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.CreateChannelApplicationRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.DecideChannelApplicationRequest;
import team.carrypigeon.backend.chat.domain.shared.controller.error.ApiErrorResponse;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 频道申请 HTTP 入口。
 * 职责：承接入群申请创建、申请列表读取与申请审批协议。
 * 边界：只做协议映射与领域 API 调用，不承载申请状态流转和审批权限规则。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "频道申请", description = "入群申请与审批能力。")
public class ChannelApplicationController {

    private final ChannelApplicationApi channelApplicationApi;

    public ChannelApplicationController(
        ChannelApplicationApi channelApplicationFlowDomainApi
    ) {
        this.channelApplicationApi = channelApplicationFlowDomainApi;
    }

    /**
     * 为当前账号创建入群申请。
     * 副作用：持久化一条 PENDING 申请记录，并替换该账号在同一频道的历史申请记录。
     *
     * @param channelId 目标频道 ID
     * @param request 入群申请请求
     * @param cpPrincipal 当前认证用户
     * @return 新建申请结果
     */
    @PostMapping("/{channelId}/applications")
    @Operation(
            summary = "申请加入频道",
            description = "为当前账号创建入群申请。同一账号在同一频道只保留一条申请记录，重复提交会替换已审批的历史记录。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "请求体仅含 reason：必填但允许空字符串，超过长度上限或缺失时返回 422",
                    content = @Content(schema = @Schema(implementation = CreateChannelApplicationRequest.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为新建申请，含 application_id、cid、uid、reason、apply_time、status",
                    content = @Content(schema = @Schema(implementation = ChannelApplicationResponse.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在，reason 为 not_found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "请求体字段非法、当前账号已是成员或已存在待处理申请",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ChannelApplicationResponse createApplication(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Valid @RequestBody CreateChannelApplicationRequest request,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return toResponse(channelApplicationApi.createChannelApplication(cpPrincipal.accountId(), channelId, request.reason()));
    }

    /**
     * 查询指定频道的入群申请列表。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色。
     *
     * @param channelId 目标频道 ID
     * @param cpPrincipal 当前认证用户
     * @return 入群申请列表响应
     */
    @GetMapping("/{channelId}/applications")
    @Operation(summary = "获取入群申请列表", description = "按频道读取入群申请列表，要求 OWNER 或 ADMIN 成员角色。无申请时 items 为空数组。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为申请列表外壳，items 为申请数组",
                    content = @Content(schema = @Schema(implementation = ChannelApplicationListResponse.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或角色不足，reason 为 not_channel_member 或 channel_invite_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ChannelApplicationListResponse listApplications(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        List<ChannelApplicationResponse> items = channelApplicationApi
            .listChannelApplications(cpPrincipal.accountId(), channelId).stream()
            .map(this::toResponse)
            .toList();
        return new ChannelApplicationListResponse(items);
    }

    /**
     * 审批入群申请。
     * 副作用：更新申请状态；批准时新增频道成员关系。
     * 约束：申请必须归属于路径声明的频道且处于待处理状态，调用方必须具备该频道 OWNER 或 ADMIN 成员角色。
     *
     * @param channelId 申请所属频道 ID
     * @param applicationId 目标申请 ID
     * @param request 审批请求
     * @param cpPrincipal 当前认证用户
     * @return 审批后的申请结果
     */
    @PostMapping("/{channelId}/applications/{applicationId}/decisions")
    @Operation(
            summary = "审批入群申请",
            description = "按 approve 或 reject 处理待审批申请，要求 OWNER 或 ADMIN 成员角色。"
                    + "批准时同时新增一条 MEMBER 成员关系。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "请求体仅含 decision：只接受 approve 或 reject，其余取值返回 422",
                    content = @Content(schema = @Schema(implementation = DecideChannelApplicationRequest.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "响应体为审批后的申请，status 为 accepted 或 declined",
                    content = @Content(schema = @Schema(implementation = ChannelApplicationResponse.class))),
            @ApiResponse(responseCode = "401", description = "未认证，响应体为标准错误包装",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "非频道成员或角色不足，reason 为 not_channel_member 或 channel_invite_forbidden",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "频道不存在，或申请不属于该频道（按不存在处理以免泄露跨频道申请存在性）",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "申请已被处理（reason 为 application_already_processed），"
                    + "或申请人已是频道成员（reason 为 conflict）",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "审批决定不是 approve 或 reject，reason 为 validation_failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ChannelApplicationResponse decideApplication(
        @Parameter(description = "申请所属频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Parameter(description = "目标申请 ID", example = "3001")
        @PathVariable @Positive(message = "applicationId must be greater than 0") long applicationId,
        @Valid @RequestBody DecideChannelApplicationRequest request,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return toResponse(channelApplicationApi.decideChannelApplication(
            cpPrincipal.accountId(),
            channelId,
            applicationId,
            request.decision()
        ));
    }

    private ChannelApplicationResponse toResponse(ChannelApplicationResult result) {
        return new ChannelApplicationResponse(
            IdUtil.toString(result.applicationId()),
            IdUtil.toString(result.channelId()),
            IdUtil.toString(result.accountId()),
            result.reason(),
            result.applyTime().toEpochMilli(),
            result.status().toLowerCase()
        );
    }
}
