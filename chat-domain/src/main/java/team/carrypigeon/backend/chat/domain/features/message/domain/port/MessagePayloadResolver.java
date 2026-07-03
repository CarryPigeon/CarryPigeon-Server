package team.carrypigeon.backend.chat.domain.features.message.domain.port;

/**
 * 消息载荷出站解析端口。
 * 职责：为消息查询与实时发布提供领域侧稳定的出站 payload 解析能力。
 * 边界：领域服务只依赖该端口，具体存储、share key 或协议字段转换由适配实现完成。
 */
public interface MessagePayloadResolver {

    /**
     * 解析出站消息载荷。
     *
     * @param messageType 消息类型
     * @param payload 已持久化的 canonical payload
     * @return 可对外返回或下发的 payload
     */
    String resolve(String messageType, String payload);
}
