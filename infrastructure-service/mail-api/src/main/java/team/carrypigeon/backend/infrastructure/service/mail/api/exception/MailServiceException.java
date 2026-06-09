package team.carrypigeon.backend.infrastructure.service.mail.api.exception;

/**
 * 邮件服务异常。
 * 职责：表达 mail-api 能力执行失败的稳定异常语义。
 * 边界：不直接暴露 SMTP 客户端或 Jakarta Mail 底层异常类型。
 */
public class MailServiceException extends RuntimeException {

    public MailServiceException(String message) {
        super(message);
    }

    public MailServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
