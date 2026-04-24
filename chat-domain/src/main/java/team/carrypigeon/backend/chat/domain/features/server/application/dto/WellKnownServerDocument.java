package team.carrypigeon.backend.chat.domain.features.server.application.dto;

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
 */
public record WellKnownServerDocument(
        String serverId,
        String serverName,
        boolean registerEnabled,
        List<String> loginMethods
) {
}
