package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text.CPChannelTextMessageData;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * HTTP 发送 canonical 频道消息请求。
 * 边界：mentions 只接受提醒用户 ID；所有 domain 专属字段必须位于 data。
 */
public record SendChannelMessageRequest(
    @Schema(description = "消息 domain，格式为主分类:子分类:版本号", example = "Core:Text:V1")
    @NotBlank(message = "domain must not be blank")
    @Size(max = 128, message = "domain length must be less than or equal to 128")
    @Pattern(
        regexp = "^[^:\\s]+:[^:\\s]+:[^:\\s]+$",
        message = "domain must use category:subcategory:version format"
    )
    String domain,
    @Schema(description = "domain 专属数据；文本 reply_to_mid 为正数 ID 字符串，省略或 null 表示不回复")
    @NotNull(message = "data must not be null")
    Map<String, Object> data,
    @ArraySchema(
        arraySchema = @Schema(description = "需要提醒的用户 ID 数组，元素为十进制字符串"),
        schema = @Schema(type = "string", example = "1001")
    )
    List<@Positive(message = "mentions must contain positive snowflake ids") Long> mentions
) {

    public Map<String, Object> toDomainData() {
        if (!CPChannelTextMessageData.DOMAIN.equals(domain.trim()) || data == null) {
            return data;
        }
        Map<String, Object> result = new LinkedHashMap<>(data);
        Object replyValue = data.get("reply_to_mid");
        if (replyValue == null) {
            result.put("reply_to_mid", null);
            return result;
        }
        if (replyValue instanceof String replyId && replyId.matches("[0-9]+")) {
            try {
                long parsedId = Long.parseLong(replyId);
                if (parsedId > 0) {
                    result.put("reply_to_mid", parsedId);
                    return result;
                }
            } catch (NumberFormatException ignored) {
                // 超出 long 范围的请求 ID 也是参数错误。
            }
        }
        String message = "reply_to_mid must be a positive decimal ID string or null";
        throw ProblemException.of(MessageProblem.INVALID_INPUT, message, Map.of(
            "field_errors", List.of(Map.of("field", "data.reply_to_mid", "reason", "invalid", "message", message))
        ));
    }
}
