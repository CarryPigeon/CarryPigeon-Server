package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.ChannelResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;

/**
 * 频道 HTTP 入口。
 * 职责：提供默认频道查询协议能力。
 * 边界：只承接协议层请求，不承载频道业务规则。
 */
@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelApplicationService channelApplicationService;
    private final AuthRequestContext authRequestContext;

    public ChannelController(
            ChannelApplicationService channelApplicationService,
            AuthRequestContext authRequestContext
    ) {
        this.channelApplicationService = channelApplicationService;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 查询当前服务端默认频道。
     *
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的默认频道结果
     */
    @GetMapping("/default")
    public CPResponse<ChannelResponse> getDefaultChannel(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        ChannelResult result = channelApplicationService.getDefaultChannel(new GetDefaultChannelCommand(principal.accountId()));
        return CPResponse.success(new ChannelResponse(
                result.channelId(),
                result.conversationId(),
                result.name(),
                result.type(),
                result.defaultChannel(),
                result.createdAt(),
                result.updatedAt()
        ));
    }
}
