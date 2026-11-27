package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新频道资料请求。
 * 职责：承载频道名称与简介两个可更新字段。
 * 边界：只做字段承载与协议校验，不承载 OWNER/ADMIN 角色判断；头像与系统频道标记不在更新范围内。
 */
public record UpdateChannelProfileRequest(
        @Schema(description = "频道名称", example = "New Name")
        @NotBlank(message = "name must not be blank")
        @Size(max = 128, message = "name length must be less than or equal to 128")
        String name,
        @Schema(description = "频道简介", example = "...")
        @NotNull(message = "brief must not be null")
        String brief
) {
}
