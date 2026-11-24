package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageIdsRequest;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessageResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.ChannelMessagesResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.dto.SendChannelMessageRequest;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;

import java.util.Arrays;
import java.util.List;

/**
 * 频道消息命令 HTTP 入口。
 * 职责：提供频道消息发送、单条查询与撤回能力。
 * 边界：不承载历史、锚点上下文或搜索查询。
 */
@RestController
@RequestMapping("/api/channels")
@PreAuthorize("isAuthenticated()")
@Tag(name = "频道消息", description = "频道消息发送与撤回能力。")
public class ChannelMessageController {

    private final ChannelMessageApi channelMessageApi;
    private final ChannelMessageContextApi messageContextApi;

    public ChannelMessageController(ChannelMessageApi channelMessageApi, ChannelMessageContextApi messageContextApi) {
        this.channelMessageApi = channelMessageApi;
        this.messageContextApi = messageContextApi;
    }

    /**
     * 发送消息。
     *
     * <p>请求 {@code POST /api/channels/2001/messages}。JSON 请求体示例 {@code {"domain":"Core:Text:V1","data":{"content":"Hello"},"mentions":[]}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelMessageResponse}。
     * <p>失败示例：禁言 30006，domain 不支持 30008。
     *
     * @param channelId   目标频道 ID
     * @param requestBody 消息体
     * @param cpPrincipal 当前认证用户
     */
    @PostMapping(value = "/{channelId}/messages", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(
        summary = "发送消息",
        description = "按 canonical envelope 发送频道消息；domain 格式为主分类:子分类:版本号，data 由对应消息处理器校验。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "消息发送成功", useReturnTypeSchema = true))
    public Result sendChannelMessage(
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @Valid
        @NotNull(message = "request body must not be null")
        @RequestBody
        SendChannelMessageRequest requestBody,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        ChannelMessageResult result = channelMessageApi.sendChannelMessage(
            cpPrincipal.accountId(),
            channelId,
            requestBody.domain().trim(),
            requestBody.data(),
            requestBody.mentions()
        );
        return Result.success(ChannelMessageResponse.from(result));
    }

    /**
     * 查询频道中的单条消息，已撤回时返回消息的当前状态。
     *
     * <p>请求 {@code GET /api/channels/2001/messages/3001}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelMessageResponse}。
     * <p>失败示例：消息不存在 30001。
     *
     * @param messageId 消息id
     */
    @GetMapping(value = "/messages/{messageId}", produces = "application/json")
    @Operation(
        summary = "查询消息",
        description = "频道成员按消息 ID 查询当前状态；跨频道消息按不存在处理。"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 data 为 ChannelMessageResponse，失败时返回五位错误码", useReturnTypeSchema = true))
    public Result getMessageById(
        @PathVariable
        @Positive(message = "messageId must be greater than 0")
        long messageId
    ){
        return Result.success(ChannelMessageResponse.from(messageContextApi.getMessage(messageId)));
    }

    /**
     * 根据消息ids查询信息
     * <p>请求 {@code POST /api/channels/2001/messages/ids}。JSON 请求体示例 {@code {"message_ids":[3001,3002]}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code ChannelMessageResponse}。
     * <p>失败示例：消息不存在 30001。
     *
     * @param requestBody 消息 ID 列表
     */
    @PostMapping(value = "/messages/ids", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(
        summary = "查询消息",
        description = "成员按消息 ID 列表查询当前状态"
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 data 为 ChannelMessageResponse，失败时返回五位错误码", useReturnTypeSchema = true))
    public Result getMessagesByIds(
        @Valid
        @NotNull(message = "request body must not be null")
        @RequestBody
        ChannelMessageIdsRequest requestBody
    ) {
        // 转化为long[]
        List<Long> messageIds = Arrays.stream(requestBody.getMessageIds()).mapToLong(Long::parseLong).boxed().toList();
        return Result.success(ChannelMessagesResponse.of(messageContextApi.getMessages(messageIds)));
    }
    /**
     * 撤回指定频道消息。
     *
     * <p>请求 {@code POST /api/channels/2001/messages/3001/recall}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：消息不存在 30001，无权撤回 30007。
     *
     * @param channelId   频道 ID
     * @param messageId   消息 ID
     * @param cpPrincipal 当前认证用户
     */
    @PostMapping(value = "/{channelId}/messages/{messageId}/recall",
        produces = "application/json")
    @Operation(summary = "撤回消息",
        description = "按频道与消息 ID 撤回指定消息，成功时 data 为 null。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result recallChannelMessage(
        @PathVariable
        @Positive(message = "channelId must be greater than 0")
        long channelId,
        @PathVariable
        @Positive(message = "messageId must be greater than 0")
        long messageId,
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        channelMessageApi.recallChannelMessage(cpPrincipal.accountId(), channelId, messageId);
        return Result.success();
    }
}
