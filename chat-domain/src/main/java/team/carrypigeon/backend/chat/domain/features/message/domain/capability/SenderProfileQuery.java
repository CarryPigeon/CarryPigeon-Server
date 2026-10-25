package team.carrypigeon.backend.chat.domain.features.message.domain.capability;

/**
 * 消息发送者资料查询能力。
 * 职责：为消息视图提供发送者展示字段，隔离 user feature 的资料类型。
 * 边界：只返回消息视图需要的展示字段，不暴露资料维护能力或 user feature 内部类型。
 */
public interface SenderProfileQuery {

    /**
     * 按账号 ID 查询发送者展示名称。
     * 失败语义：资料不存在时由实现抛出领域问题异常。
     *
     * @param accountId 目标账号 ID
     * @return 发送者展示名称
     */
    String findSenderName(long accountId);
}
