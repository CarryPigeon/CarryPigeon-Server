package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息发送命令校验协作对象。
 * 职责：处理消息发送入口的基础字段校验。
 * 边界：不访问仓储、不判断频道权限、不创建或发布消息。
 */
class MessageDeliveryCommandValidator {

    void validateSystemSendCommand(SendSystemChannelMessageCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        validateCanonicalFields(command.domainVersion(), command.data());
    }

    void validateSendCommand(SendChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        if (command.domain() == null || command.domain().isBlank()) {
            throw ProblemException.validationFailed("domain must not be blank");
        }
        validateCanonicalFields(command.domainVersion(), command.data());
    }

    private void validateCanonicalFields(String domainVersion, java.util.Map<String, Object> data) {
        if (domainVersion == null || domainVersion.isBlank()) {
            throw ProblemException.validationFailed("domain_version must not be blank");
        }
        if (data == null) {
            throw ProblemException.validationFailed("data must not be null");
        }
        if (data.containsKey("mentions")) {
            throw ProblemException.validationFailed("mentions must be top-level message metadata");
        }
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
