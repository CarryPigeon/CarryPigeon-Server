package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.CreateChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.DecideChannelApplicationCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelApplicationsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelApplicationListResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelApplicationResponse;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.CreateChannelApplicationRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.DecideChannelApplicationRequest;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 频道申请 HTTP 入口。
 */
@RestController
@RequestMapping("/api/channels")
@Tag(name = "频道申请", description = "入群申请与审批能力。")
public class ChannelApplicationController {

    private final ChannelApplicationService channelApplicationService;
    private final AuthRequestContext authRequestContext;

    public ChannelApplicationController(ChannelApplicationService channelApplicationService, AuthRequestContext authRequestContext) {
        this.channelApplicationService = channelApplicationService;
        this.authRequestContext = authRequestContext;
    }

    @PostMapping("/{channelId}/applications")
    @Operation(summary = "申请加入频道", description = "为当前账户创建入群申请。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回申请结果")})
    public ChannelApplicationResponse createApplication(
            @PathVariable long channelId,
            @Valid @RequestBody CreateChannelApplicationRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(servletRequest);
        return toResponse(channelApplicationService.createChannelApplication(new CreateChannelApplicationCommand(
                principal.accountId(),
                channelId,
                request.reason()
        )));
    }

    @GetMapping("/{channelId}/applications")
    @Operation(summary = "获取入群申请列表", description = "按频道读取入群申请。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回申请列表")})
    public ChannelApplicationListResponse listApplications(@PathVariable long channelId, HttpServletRequest servletRequest) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(servletRequest);
        List<ChannelApplicationResponse> items = channelApplicationService.listChannelApplications(new ListChannelApplicationsQuery(principal.accountId(), channelId)).stream()
                .map(this::toResponse)
                .toList();
        return new ChannelApplicationListResponse(items);
    }

    @PostMapping("/{channelId}/applications/{applicationId}/decisions")
    @Operation(summary = "审批入群申请", description = "approve/reject 入群申请。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "返回审批后的申请结果")})
    public ChannelApplicationResponse decideApplication(
            @PathVariable long channelId,
            @PathVariable long applicationId,
            @Valid @RequestBody DecideChannelApplicationRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(servletRequest);
        return toResponse(channelApplicationService.decideChannelApplication(new DecideChannelApplicationCommand(
                principal.accountId(),
                channelId,
                applicationId,
                request.decision()
        )));
    }

    private ChannelApplicationResponse toResponse(ChannelApplicationResult result) {
        return new ChannelApplicationResponse(
                Ids.toString(result.applicationId()),
                Ids.toString(result.channelId()),
                Ids.toString(result.accountId()),
                result.reason(),
                result.applyTime().toEpochMilli(),
                result.status().toLowerCase()
        );
    }
}
