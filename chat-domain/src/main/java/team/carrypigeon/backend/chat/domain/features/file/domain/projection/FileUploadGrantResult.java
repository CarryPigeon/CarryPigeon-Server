package team.carrypigeon.backend.chat.domain.features.file.domain.projection;

import java.time.Instant;

/**
 * 文件上传授权结果。
 * 职责：向协议层暴露上传文件所需的稳定结果。
 * 边界：不承载 HTTP 包装逻辑。
 *
 * @param fileId 文件 ID
 * @param shareKey 分享键
 * @param uploadUrl 同源上传 URL
 * @param expiresAt 上传入口过期时间
 */
public record FileUploadGrantResult(long fileId, String shareKey, String uploadUrl, Instant expiresAt) {
}
