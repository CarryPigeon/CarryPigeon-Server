package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建会话并签发令牌请求。
 * 职责：承载 `POST /api/auth/tokens` 的最小输入。
 * 边界：当前仅支持 `email_code` 授权类型。
 */
public record CreateTokenSessionRequest(
        @Schema(description = "授权类型", example = "email_code")
        @NotBlank(message = "grant_type must not be blank")
        String grantType,
        @Schema(description = "目标邮箱", example = "user@example.com")
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email must not be blank")
        String email,
        @Schema(description = "邮箱验证码", example = "123456")
        @NotBlank(message = "code must not be blank")
        String code,
        @Schema(description = "客户端上下文")
        @Valid @NotNull(message = "client must not be null")
        ClientRequest client
) {

    /**
     * 创建会话时的客户端上下文。
     * 职责：携带设备标识和当前设备插件安装态。
     */
    public record ClientRequest(
            @Schema(description = "稳定设备标识", example = "a-stable-device-id")
            @NotBlank(message = "device_id must not be blank")
            String deviceId,
            @Schema(description = "当前设备已安装插件列表")
            @Valid
            List<InstalledPluginRequest> installedPlugins
    ) {
    }

    /**
     * 当前设备已安装插件描述。
     * 职责：供 required plugin gate 判断客户端能力缺口。
     */
    public record InstalledPluginRequest(
            @Schema(description = "插件标识", example = "mc-bind")
            @NotBlank(message = "plugin_id must not be blank")
            String pluginId,
            @Schema(description = "插件版本", example = "1.2.0")
            String version
    ) {
    }
}
