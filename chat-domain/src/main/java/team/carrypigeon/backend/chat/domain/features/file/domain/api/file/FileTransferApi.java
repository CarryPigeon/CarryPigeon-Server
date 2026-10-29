package team.carrypigeon.backend.chat.domain.features.file.domain.api.file;

import java.io.InputStream;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileUploadGrantResult;

/**
 * 文件传输领域 API。
 * 职责：暴露上传授权、上传写入和文件下载能力。
 * 边界：不暴露 controller 协议、对象存储适配实现和 share_key 解析细节。
 */
public interface FileTransferApi {

    /**
     * 创建文件上传授权。
     *
     * @param accountId 上传发起账号 ID
     * @param sizeBytes 文件大小，单位字节
     * @return 文件上传授权投影
     */
    FileUploadGrantResult createUploadGrant(long accountId, long sizeBytes);

    /**
     * 使用已签发 share key 上传文件内容。
     *
     * @param accountId 上传账号 ID
     * @param shareKey 上传授权 share key
     * @param contentType 实际上传内容类型
     * @param sizeBytes 实际上传大小，单位字节
     * @param content 文件内容输入流，由调用方负责提供可读取流
     */
    void uploadFile(long accountId, String shareKey, String contentType, long sizeBytes, InputStream content);

    /**
     * 根据 share key 创建文件下载结果。
     *
     * @param shareKey 下载 share key
     * @return 可下载时返回下载结果投影，不可下载时返回空
     */
    Optional<FileDownloadResult> downloadFile(String shareKey);

}
