package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.SendChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;

/**
 * 频道消息命令 HTTP 入口。
 * 职责：提供频道消息发送与撤回能力。
 * 边界：不承载历史、锚点上下文或搜索查询。
 */
@Validated
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息发送与撤回能力。")
public class ChannelMessageController {

    private final ChannelMessageApi channelMessageApi;

    public ChannelMessageController(ChannelMessageApi channelMessageApi) {
        this.channelMessageApi = channelMessageApi;
    }

    /**
     * 发送消息。
     *
     * @param channelId   目标频道 ID
     * @param requestBody 消息体
     * @param cpPrincipal 当前认证用户
     * @return 创建后的消息响应
     */
    @PostMapping("/{channelId}/messages")
    @Operation(
        summary = "发送消息",
        description = "按 canonical envelope 发送频道消息；domain 格式为主分类:子分类:版本号，data 由对应消息处理器校验。"
    )
    @ApiResponses({@ApiResponse(responseCode = "201", description = "消息发送成功")})
    public ResponseEntity<ChannelMessageResponse> sendChannelMessage(
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @Valid @NotNull(message = "request body must not be null") @RequestBody SendChannelMessageRequest requestBody,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        ChannelMessageResult result = channelMessageApi.sendChannelMessage(
            cpPrincipal.accountId(),
            channelId,
            requestBody.domain().trim(),
            requestBody.data(),
            requestBody.mentions()
        );
        return ResponseEntity.status(201).body(ChannelMessageResponse.from(result));
    }

    /**
     * 撤回指定频道消息。
     * 输出：成功时返回 204，无响应体。
     *
     * @param channelId   频道 ID
     * @param messageId   消息 ID
     * @param cpPrincipal 当前认证用户
     * @return 无内容响应
     */
    @PostMapping("/{channelId}/messages/{messageId}/recall")
    @Operation(summary = "撤回消息", description = "按频道与消息 ID 撤回指定消息，成功返回 204。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "消息撤回成功，响应体为空")})
    public ResponseEntity<Void> recallChannelMessage(
        @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
        @PathVariable @Positive(message = "messageId must be greater than 0") long messageId,
        @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        channelMessageApi.recallChannelMessage(cpPrincipal.accountId(), channelId, messageId);
        return ResponseEntity.noContent().build();
    }
}
