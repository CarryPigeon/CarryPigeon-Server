package team.carrypigeon.backend.chat.domain.features.file.domain.projection;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * 文件下载结果。
 * 职责：向协议层表达文件下载的领域结果，隐藏对象存储契约模型。
 * 边界：只描述直接内容流或重定向 URL，不承载 HTTP status 选择。
 *
 * @param contentType 文件内容类型
 * @param size 文件大小
 * @param content 直接可返回的内容流
 * @param redirectUrl 需要重定向访问的下载 URL
 */
public record FileDownloadResult(String contentType, long size, Optional<InputStream> content, Optional<URI> redirectUrl) {
}
