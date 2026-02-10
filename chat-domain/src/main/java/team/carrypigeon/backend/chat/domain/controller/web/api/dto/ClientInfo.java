package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 客户端设备与插件信息。
 *
 * @param deviceId 设备 ID。
 * @param installedPlugins 客户端已安装插件列表。
 */
public record ClientInfo(@NotBlank String deviceId, @Valid List<InstalledPlugin> installedPlugins) {
}
