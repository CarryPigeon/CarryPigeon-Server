package team.carrypigeon.backend.api.chat.domain.message;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 消息数据协议。
 * <p>
 * 不同消息领域实现该接口以提供解析、摘要与领域标识能力。
 */
public interface CPMessageData {

    /**
     * 解析消息数据。
     *
     * @param data 消息 JSON 载荷。
     * @return 解析后的消息对象；解析失败返回 {@code null}。
     */
    CPMessageData parse(JsonNode data);

    /**
     * 获取消息摘要内容。
     *
     * @return 可用于列表预览或通知展示的摘要文本。
     */
    String getSContent();

    /**
     * 获取结构化消息数据。
     *
     * @return 消息 JSON 载荷。
     */
    JsonNode getData();

    /**
     * 获取消息领域。
     *
     * @return 消息领域标识。
     */
    String getDomain();
}
