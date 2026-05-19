package team.carrypigeon.backend.chat.domain.features.server.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 服务端公开源信息文档。
 * 职责：向匿名调用方暴露最小可用的服务端公开摘要信息。
 * 边界：只承载当前仓库已稳定支持的公开字段，不扩展未来能力占位字段。
 *
 * @param serverId 当前服务端稳定标识
 * @param serverName 当前服务端公开名称
 * @param registerEnabled 是否允许注册
 * @param loginMethods 当前支持的登录方式列表
 * @param publicCapabilities 当前服务端已稳定公开的能力标识列表
 * @param publicPlugins 当前服务端已公开的最小插件/消息类型标识列表
 */
public record WellKnownServerDocument(
        @Schema(description = "当前服务端稳定标识", example = "carrypigeon-local")
        String serverId,
        @Schema(description = "当前服务端公开名称", example = "CarryPigeonBackend")
        String serverName,
        @Schema(description = "是否允许新用户注册", example = "true")
        boolean registerEnabled,
        @Schema(description = "支持的登录方式列表", example = "[\"username_password\"]")
        List<String> loginMethods,
        @Schema(description = "对匿名调用方公开的能力标识列表", example = "[\"user_registration\",\"username_password_login\"]")
        List<String> publicCapabilities,
        @Schema(description = "公开的插件或消息类型标识列表", example = "[\"custom\",\"file\",\"text\",\"voice\"]")
        List<String> publicPlugins
) {
}
