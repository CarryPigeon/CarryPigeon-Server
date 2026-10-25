package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

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
    @Schema(description = "domain 专属 canonical 数据")
    @NotNull(message = "data must not be null")
    Map<String, Object> data,
    @ArraySchema(
        arraySchema = @Schema(description = "需要提醒的用户 ID 数组，元素为十进制字符串"),
        schema = @Schema(type = "string", example = "1001")
    )
    List<@Positive(message = "mentions must contain positive snowflake ids") Long> mentions
) {
}
