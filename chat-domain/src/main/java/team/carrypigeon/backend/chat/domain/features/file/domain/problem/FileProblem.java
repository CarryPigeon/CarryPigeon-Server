package team.carrypigeon.backend.chat.domain.features.file.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 文件 feature 的错误码表。
 * 职责：声明文件上传、下载与分享密钥相关的失败语义。
 * 边界：只服务文件 feature，不引用其它 feature 的码表。
 */
public enum FileProblem implements ProblemMapping {

    /** 文件相关入参不合法。 */
    INVALID_INPUT(10001, 422, "validation_failed", "validation failed"),

    /** 原始上传协议要求明确的 Content-Length。 */
    LENGTH_REQUIRED(10007, 411, "length_required", "Content-Length is required"),

    /** 申请大小或上传声明超过部署的单文件上限。 */
    FILE_TOO_LARGE(40001, 413, "file_too_large", "file size exceeds the upload limit"),

    /** 文件分享密钥非法或已失效。 */
    INVALID_SHARE_KEY(40002, 422, "invalid_share_key", "share_key is invalid"),

    /** 目标文件不存在。 */
    FILE_NOT_FOUND(40003, 404, "not_found", "file does not exist"),

    /** 对象存储服务未启用或不可用。 */
    STORAGE_UNAVAILABLE(10011, 503, "storage_service_unavailable", "storage service is unavailable"),

    /** 声明大小加上现有占用会突破账号总空间。 */
    QUOTA_EXCEEDED(40004, 409, "file_quota_exceeded", "account file storage quota exceeded"),

    /** 未完成上传（包括尚未使用的授权）达到账号上限。 */
    UPLOAD_LIMIT_REACHED(40005, 429, "upload_limit_reached", "too many unfinished uploads"),

    /** 已有写入者，或者完成后重试的内容不同。 */
    UPLOAD_CONFLICT(40006, 409, "upload_conflict", "upload is in progress or completed with different content"),

    /** 请求体实际字节数与授权声明不一致。 */
    UPLOAD_SIZE_MISMATCH(40007, 422, "upload_size_mismatch", "upload size does not match the declared size"),

    /** 上传内容读取失败。 */
    UPLOAD_READ_FAILED(10012, 500, "internal_error", "internal server error"),

    /** 分享密钥签名失败。 */
    SHARE_KEY_SIGN_FAILED(10012, 500, "internal_error", "internal server error");

    private final int code;
    private final int status;
    private final String reason;
    private final String message;

    FileProblem(int code, int status, String reason, String message) {
        this.code = code;
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public String message() {
        return message;
    }
}
