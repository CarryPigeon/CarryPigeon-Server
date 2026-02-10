package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 客户端已安装插件描述。
 *
 * @param pluginId 插件 ID。
 * @param version 插件版本。
 */
public record InstalledPlugin(@NotBlank String pluginId, String version) {
}
