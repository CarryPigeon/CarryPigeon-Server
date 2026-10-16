package team.carrypigeon.backend.chat.domain.features.channel.controller.http.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.ChannelApplicationApi;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.ChannelApplicationListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.ChannelApplicationResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.CreateChannelApplicationRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.DecideChannelApplicationRequest;
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
    @Operation(summary = "申请加入频道", description = "为当前账号创建入群申请，重复申请会替换未处理的历史申请。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回新建的入群申请结果"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "频道不允许提交入群申请"),
            @ApiResponse(responseCode = "404", description = "频道不存在"),
            @ApiResponse(responseCode = "422", description = "请求体字段非法、当前账号已是频道成员或已存在待处理申请")
    })
    public ChannelApplicationResponse createApplication(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Valid @RequestBody CreateChannelApplicationRequest request,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return toResponse(channelApplicationApi.createChannelApplication(new CreateChannelApplicationCommand(
            cpPrincipal.accountId(),
            channelId,
            request.reason()
        )));
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
    @Operation(summary = "获取入群申请列表", description = "按频道读取入群申请列表，要求 OWNER 或 ADMIN 成员角色。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回入群申请列表"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "当前账号不是频道成员或角色不足"),
            @ApiResponse(responseCode = "404", description = "频道不存在")
    })
    public ChannelApplicationListResponse listApplications(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        List<ChannelApplicationResponse> items = channelApplicationApi
            .listChannelApplications(new ListChannelApplicationsQuery(cpPrincipal.accountId(), channelId)).stream()
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
    @Operation(summary = "审批入群申请", description = "按 approve 或 reject 处理待审批申请，要求 OWNER 或 ADMIN 成员角色。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回审批后的申请结果"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "当前账号不是频道成员或角色不足"),
            @ApiResponse(responseCode = "404", description = "频道不存在或申请不属于该频道"),
            @ApiResponse(responseCode = "409", description = "申请已被处理"),
            @ApiResponse(responseCode = "422", description = "审批决定非法或申请人已是频道成员")
    })
    public ChannelApplicationResponse decideApplication(
        @Parameter(description = "申请所属频道 ID", example = "2001")
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Parameter(description = "目标申请 ID", example = "3001")
        @PathVariable @Positive(message = "applicationId must be greater than 0") long applicationId,
        @Valid @RequestBody DecideChannelApplicationRequest request,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        return toResponse(channelApplicationApi.decideChannelApplication(new DecideChannelApplicationCommand(
            cpPrincipal.accountId(),
            channelId,
            applicationId,
            request.decision()
        )));
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
