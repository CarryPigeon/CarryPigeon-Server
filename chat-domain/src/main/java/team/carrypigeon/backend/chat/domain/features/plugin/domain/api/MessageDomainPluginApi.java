package team.carrypigeon.backend.chat.domain.features.plugin.domain.api;

import team.carrypigeon.backend.chat.domain.features.plugin.domain.command.ValidateMessageDataCommand;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.ValidatedMessageDataResult;

/**
 * 消息 domain 插件 API。
 * 职责：为消息发布 feature 提供统一 domain/version/data 校验与规范化入口。
 * 边界：不暴露插件实例、注册表或插件内部服务。
 * 输入：服务端生成的消息上下文、domain、版本、不可信 data 与入口类型。
 * 输出：已规范化的 canonical data、domain、版本和派生 preview。
 * 失败语义：domain 未注册、版本不支持、data 异常或入口无权发送时抛出领域问题异常。
 * 调用方：message feature 的消息发布用例。
 */
public interface MessageDomainPluginApi {

    /**
     * 校验并规范化消息 domain data。
     *
     * @param command 插件校验命令
     * @return 已验证的 canonical 消息内容
     */
    ValidatedMessageDataResult validateMessageData(ValidateMessageDataCommand command);

    /**
     * 判断扩展消息类型是否已注册并允许使用。
     *
     * @param messageType 扩展消息类型
     * @return 已注册时为 true
     */
    boolean supportsExtensionMessageType(String messageType);
}
