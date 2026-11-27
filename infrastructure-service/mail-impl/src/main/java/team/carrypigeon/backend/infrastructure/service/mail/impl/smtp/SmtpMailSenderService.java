package team.carrypigeon.backend.infrastructure.service.mail.impl.smtp;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.mail.api.exception.MailServiceException;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.config.SmtpMailProperties;

/**
 * SMTP 邮件发送服务。
 * 职责：基于 Spring Mail 实现 mail-api 定义的纯文本邮件发送契约。
 * 边界：SMTP 访问细节封装在 impl 内部，不向上层泄露。
 */
@EnvironmentComponent(profiles = PROD)
@Slf4j
public class SmtpMailSenderService implements MailSenderService {

    private final JavaMailSender mailSender;
    private final SmtpMailProperties properties;
    private final Executor executor;

    public SmtpMailSenderService(JavaMailSender mailSender, SmtpMailProperties properties, @Qualifier("mailExecutor") Executor executor) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.executor = executor;
    }

    /**
     * 发送纯文本邮件。
     * 输入：收件人、标题和正文。
     * 失败通过完成结果传播；队列拒绝同步抛出，不能切换到请求线程发送。
     * 发送失败最多尝试三次，认证和消息准备错误不重试。SMTP 超时可能导致同一封邮件被重复接受。
     * 任务仅保存在内存，正常关闭等待已受理任务；进程异常退出后不自动恢复。
     *
     * @param command 邮件发送命令
     */
    @Override
    public CompletionStage<Void> send(MailSendCommand command) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.fromAddress());
        message.setTo(command.to().trim());
        message.setSubject(command.subject().trim());
        message.setText(command.text());
        return CompletableFuture.runAsync(() -> {
            try {
                sendWithRetries(message);
            } catch (RuntimeException | Error failure) {
                // SMTP 异常消息可能包含收件地址，仅记录根因类型。
                log.atError()
                    .addKeyValue("stage", "delivery")
                    .addKeyValue("outcome", "failure")
                    .addKeyValue("failure_type", NestedExceptionUtils.getMostSpecificCause(failure).getClass().getName())
                    .log("SMTP 邮件投递失败");
                throw failure;
            }
            log.atDebug()
                .addKeyValue("stage", "delivery")
                .addKeyValue("outcome", "success")
                .log("SMTP 服务器已接受邮件");
        }, executor);
    }

    private void sendWithRetries(SimpleMailMessage message) {
        for (int attempt = 1; true; attempt++) {
            try {
                mailSender.send(message);
                return;
            } catch (MailException exception) {
                if (!(exception instanceof MailSendException) || attempt == 3) {
                    throw new MailServiceException("failed to send email", exception);
                }
            }
        }
    }
}
