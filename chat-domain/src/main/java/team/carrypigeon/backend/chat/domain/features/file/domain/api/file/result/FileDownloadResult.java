package team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result;

import java.io.InputStream;

/**
 * 文件下载结果。
 * 职责：向协议层表达文件下载的领域结果，隐藏对象存储契约模型。
 * 边界：始终承载文件内容流，由应用服务器直接转发给客户端。
 *
 * @param contentType 文件内容类型
 * @param size 文件大小
 * @param content 文件内容流
 */
public record FileDownloadResult(String contentType, long size, InputStream content) {
}
