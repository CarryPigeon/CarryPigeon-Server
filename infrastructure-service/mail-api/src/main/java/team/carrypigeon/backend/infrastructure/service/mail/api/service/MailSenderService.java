package team.carrypigeon.backend.infrastructure.service.mail.api.service;

import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import java.util.concurrent.CompletionStage;

/**
 * 邮件发送服务抽象。
 * 职责：为上层模块提供最小纯文本邮件发送能力。
 * 边界：不暴露 SMTP 客户端、模板引擎或具体服务商 SDK。
 */
public interface MailSenderService {

    /**
     * 发送一封纯文本邮件。
     *
     * @param command 邮件发送命令
     * @return 异步投递结果；完成只表示 SMTP 服务器已接受发送，失败表示本次投递未完成
     * @throws RuntimeException 任务被拒绝或无法受理；调用方应与异步失败分别处理
     */
    CompletionStage<Void> send(MailSendCommand command);
}
