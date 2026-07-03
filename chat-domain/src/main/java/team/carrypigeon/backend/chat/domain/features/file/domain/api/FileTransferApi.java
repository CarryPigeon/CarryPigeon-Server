package team.carrypigeon.backend.chat.domain.features.file.domain.api;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileUploadGrantResult;

/**
 * 文件传输领域 API。
 * 职责：暴露上传授权、同源上传、下载和固定上传头查询能力。
 * 边界：不暴露 controller 协议、对象存储适配实现和 share_key 解析细节。
 * 输入：账号、文件名、MIME 类型、大小、share key 与文件内容流。
 * 输出：上传授权、下载授权、固定上传头或上传副作用。
 * 失败语义：文件参数非法、share key 无效、权限不足或对象不存在由领域问题异常表达。
 * 调用方：只通过本接口请求文件传输能力，不直接依赖对象存储服务。
 */
public interface FileTransferApi {

    /**
     * 创建文件上传授权。
     * 输入：上传账号、文件名、MIME 类型和文件大小。
     * 输出：包含 share key、上传目标和上传约束的授权投影。
     * 约束：文件名、类型和大小必须满足领域上传策略。
     *
     * @param accountId 上传发起账号 ID
     * @param filename 原始文件名
     * @param mimeType 文件 MIME 类型
     * @param sizeBytes 文件大小，单位字节
     * @return 文件上传授权投影
     */
    FileUploadGrantResult createUploadGrant(long accountId, String filename, String mimeType, long sizeBytes);

    /**
     * 使用已签发 share key 上传文件内容。
     * 输入：上传账号、share key、内容类型、大小和文件内容流。
     * 副作用：将文件内容写入受领域策略保护的存储位置。
     * 约束：share key 必须属于当前上传场景，内容属性必须与授权约束一致。
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
     * 输入：可选访问账号和下载 share key。
     * 输出：存在且允许访问时返回下载投影，否则返回空。
     * 约束：访问权限、share key 解析和对象存在性由领域实现统一判断。
     *
     * @param accountId 可选访问账号 ID，匿名或系统场景可为空
     * @param shareKey 下载 share key
     * @return 可下载时返回下载结果投影，不可下载时返回空
     */
    Optional<FileDownloadResult> downloadFile(Long accountId, String shareKey);

    /**
     * 判断 share key 是否指向服务头像资源。
     * 输入：待判断的 share key。
     * 输出：属于服务头像资源时返回 true。
     * 约束：调用方不应解析 share key 内部格式。
     *
     * @param shareKey 文件 share key
     * @return 是否为服务头像资源
     */
    boolean isServerAvatar(String shareKey);

    /**
     * 获取同源上传需要固定携带的请求头。
     * 输出：上传头名称到取值的稳定映射。
     * 边界：该结果表达领域上传约束，不暴露对象存储客户端实现。
     *
     * @return 上传请求头映射
     */
    Map<String, String> uploadHeaders();
}
