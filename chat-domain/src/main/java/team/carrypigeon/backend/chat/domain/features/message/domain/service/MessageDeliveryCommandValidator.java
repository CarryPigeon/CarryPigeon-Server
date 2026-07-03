package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息发送命令校验协作对象。
 * 职责：处理消息发送入口的基础字段校验。
 * 边界：不访问仓储、不判断频道权限、不创建或发布消息。
 */
class MessageDeliveryCommandValidator {

    void validateSendCommand(SendChannelTextMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        if (command.body() == null || command.body().isBlank()) {
            throw ProblemException.validationFailed("body must not be blank");
        }
    }

    void validateSystemSendCommand(SendSystemChannelMessageCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
    }

    void validateSendCommand(SendChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        if (command.draft() == null) {
            throw ProblemException.validationFailed("draft must not be null");
        }
        if (command.draft().type() == null || command.draft().type().isBlank()) {
            throw ProblemException.validationFailed("message type must not be blank");
        }
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
