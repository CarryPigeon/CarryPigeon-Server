package team.carrypigeon.backend.chat.domain.features.channel.controller.http.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.shared.page.PageResponse;
import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.ChannelApplicationResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.CreateChannelApplicationRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto.DecideChannelApplicationRequest;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.ChannelApplicationApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.result.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.shared.id.IdUtil;
import team.carrypigeon.backend.chat.domain.shared.page.PageQuery;
import team.carrypigeon.backend.chat.domain.shared.page.PageResult;

/**
 * 频道申请 HTTP 入口。
 * 职责：承接入群申请创建、申请列表读取与申请审批协议。
 * 边界：只做协议映射与领域 API 调用，不承载申请状态流转和审批权限规则。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
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
     * <p>请求 {@code POST /api/channels/2001/applications}。JSON 请求体示例 {@code {"reason":"加入项目讨论"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelApplicationResponse}。
     * <p>失败示例：频道不存在 20001。
     *
     * @param channelId 目标频道 ID
     * @param request 入群申请请求
     * @param cpPrincipal 当前认证用户
     */
    @PostMapping(value = "/{channelId}/applications", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(
            summary = "申请加入频道",
            description = "为当前账号创建入群申请。同一账号在同一频道只保留一条申请记录，重复提交会替换已审批的历史记录。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "请求体仅含 reason：必填但允许空字符串，超过长度上限或缺失时返回业务码 10001",
                    content = @Content(schema = @Schema(implementation = CreateChannelApplicationRequest.class))
            )
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为新建申请，含 application_id、cid、uid、reason、apply_time、status", useReturnTypeSchema = true))
    public Result createApplication(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Valid
        @RequestBody
        CreateChannelApplicationRequest request,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        return Result.success(toResponse(channelApplicationApi.createChannelApplication(cpPrincipal.accountId(), channelId, request.reason())));
    }

    /**
     * 个人查询自己申请的列表和详情
     * 约束：调用方必须已经登陆
     *
     * <p>请求 {@code GET /api/channels/applications?page=1&page_size=20}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code PageResponse<ChannelApplicationResponse>}。
     * <p>失败示例：角色不足 20012。
     *
     * @param cpPrincipal 当前认证用户
     * */
    @GetMapping(value = "/applications", produces = "application/json")
    @Operation(summary = "获取入群申请列表", description = "分页读取个人入群申请列表，要求 OWNER 或 ADMIN 成员角色。无申请时 items 为空数组。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为分页外壳，items 为申请数组", useReturnTypeSchema = true))
    public Result listApplications(
        @Parameter(description = "页码，从 1 开始", example = "1")
        @RequestParam(name = "page", defaultValue = "1")
        @Min(value = 1, message = "page must be greater than 0")
        int page,
        @Parameter(description = "每页条数，范围 1..50", example = "20")
        @RequestParam(name = "page_size", defaultValue = "20")
        @Min(value = 1, message = "page_size must be between 1 and 50")
        @Max(value = 50, message = "page_size must be between 1 and 50")
        int pageSize,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        PageResult<ChannelApplicationResult> result = channelApplicationApi.listMyChannelApplications(
                cpPrincipal.accountId(),
                new PageQuery(page, pageSize)
        );
        return Result.success(new PageResponse<>(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.pageSize(),
                result.total(),
                result.hasMore()
        ));
    }

    /**
     * 查询指定频道的入群申请列表。
     * 约束：调用方必须具备该频道 OWNER 或 ADMIN 成员角色。
     *
     * <p>请求 {@code GET /api/channels/2001/applications?page=1&page_size=20}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code PageResponse<ChannelApplicationResponse>}。
     * <p>失败示例：角色不足 20012。
     *
     * @param channelId 目标频道 ID
     * @param cpPrincipal 当前认证用户
     */
    @GetMapping(value = "/{channelId}/applications", produces = "application/json")
    @Operation(summary = "获取入群申请列表", description = "分页读取频道入群申请列表，要求 OWNER 或 ADMIN 成员角色。无申请时 items 为空数组。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为分页外壳，items 为申请数组", useReturnTypeSchema = true))
    public Result listApplications(
        @Parameter(description = "目标频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "页码，从 1 开始", example = "1")
        @RequestParam(name = "page", defaultValue = "1")
        @Min(value = 1, message = "page must be greater than 0")
        int page,
        @Parameter(description = "每页条数，范围 1..50", example = "20")
        @RequestParam(name = "page_size", defaultValue = "20")
        @Min(value = 1, message = "page_size must be between 1 and 50")
        @Max(value = 50, message = "page_size must be between 1 and 50")
        int pageSize,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        PageResult<ChannelApplicationResult> result = channelApplicationApi.listChannelApplications(
                cpPrincipal.accountId(),
                channelId,
                new PageQuery(page, pageSize)
        );
        return Result.success(new PageResponse<>(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.pageSize(),
                result.total(),
                result.hasMore()
        ));
    }

    /**
     * 审批入群申请。
     * 副作用：更新申请状态；批准时新增频道成员关系。
     * 约束：申请必须归属于路径声明的频道且处于待处理状态，调用方必须具备该频道 OWNER 或 ADMIN 成员角色。
     *
     * <p>请求 {@code POST /api/channels/2001/applications/5001/decisions}。JSON 请求体示例 {@code {"decision":"approve"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelApplicationResponse}。
     * <p>失败示例：申请不存在 20005，申请已处理 20016。
     *
     * @param channelId 申请所属频道 ID
     * @param applicationId 目标申请 ID
     * @param request 审批请求
     * @param cpPrincipal 当前认证用户
     */
    @PostMapping(value = "/{channelId}/applications/{applicationId}/decisions", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(
            summary = "审批入群申请",
            description = "按 approve 或 reject 处理待审批申请，要求 OWNER 或 ADMIN 成员角色。"
                    + "批准时同时新增一条 MEMBER 成员关系。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "请求体仅含 decision：只接受 approve 或 reject，其余取值返回业务码 10001",
                    content = @Content(schema = @Schema(implementation = DecideChannelApplicationRequest.class))
            )
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "data 为审批后的申请，status 为 accepted 或 declined", useReturnTypeSchema = true))
    public Result decideApplication(
        @Parameter(description = "申请所属频道 ID", example = "2001")
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Parameter(description = "目标申请 ID", example = "3001")
        @PathVariable
        @Positive(message = "applicationId must be greater than 0")
        long applicationId,
        @Valid
        @RequestBody
        DecideChannelApplicationRequest request,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        return Result.success(toResponse(channelApplicationApi.decideChannelApplication(
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
