package team.carrypigeon.backend.infrastructure.service.mail.api.service;

import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;

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
     */
    void send(MailSendCommand command);
}
