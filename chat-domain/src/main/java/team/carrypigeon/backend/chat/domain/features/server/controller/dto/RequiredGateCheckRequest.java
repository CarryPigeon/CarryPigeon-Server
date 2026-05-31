package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * required gate 预检查请求。
 * 职责：承载客户端当前设备与插件安装态，供服务端执行匿名 gate 判断。
 * 边界：只用于 precheck，不直接承载会话创建语义。
 */
public record RequiredGateCheckRequest(
        @Schema(description = "客户端上下文")
        @Valid @NotNull(message = "client must not be null")
        ClientRequest client
) {

    public record ClientRequest(
            @Schema(description = "稳定设备标识", example = "a-stable-device-id")
            @NotBlank(message = "device_id must not be blank")
            String deviceId,
            @Schema(description = "当前设备已安装插件列表")
            @Valid
            List<InstalledPluginRequest> installedPlugins
    ) {
    }

    public record InstalledPluginRequest(
            @Schema(description = "插件标识", example = "mc-bind")
            @NotBlank(message = "plugin_id must not be blank")
            String pluginId,
            @Schema(description = "插件版本", example = "1.2.0")
            String version
    ) {
    }
}
