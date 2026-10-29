package team.carrypigeon.backend.chat.domain.features.file.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 文件 feature 的错误码表。
 * 职责：声明文件上传、下载与分享密钥相关的失败语义。
 * 边界：只服务文件 feature，不引用其它 feature 的码表。
 */
public enum FileProblem implements ProblemMapping {

    /** 文件相关入参不合法。 */
    INVALID_INPUT(422, "validation_failed", "validation failed"),

    /** 文件分享密钥非法或已失效。 */
    INVALID_SHARE_KEY(422, "invalid_share_key", "share_key is invalid"),

    /** 目标文件不存在。 */
    FILE_NOT_FOUND(404, "not_found", "file does not exist"),

    /** 上传内容读取失败。 */
    UPLOAD_READ_FAILED(500, "internal_error", "internal server error"),

    /** 分享密钥签名失败。 */
    SHARE_KEY_SIGN_FAILED(500, "internal_error", "internal server error");

    private final int status;
    private final String reason;
    private final String message;

    FileProblem(int status, String reason, String message) {
        this.status = status;
        this.reason = reason;
        this.message = message;
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
