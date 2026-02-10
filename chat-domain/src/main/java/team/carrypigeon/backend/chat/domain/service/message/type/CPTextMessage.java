package team.carrypigeon.backend.chat.domain.service.message.type;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageDomain;

/**
 * 文本消息解析器与数据载体。
 * <p>
 * 约定输入结构：`{"text":"..."}`。
 */
@CPMessageDomain("Core:Text")
public class CPTextMessage implements CPMessageData {

    private static final String DOMAIN = "Core:Text";

    private String sContent;
    private JsonNode data;

    /**
     * 默认构造函数。
     */
    public CPTextMessage() {
    }

    /**
     * 构造文本消息。
     *
     * @param sContent 摘要文本。
     * @param data 原始消息数据。
     */
    public CPTextMessage(String sContent, JsonNode data) {
        this.sContent = sContent;
        this.data = data;
    }

    /**
     * 解析文本消息。
     *
     * @param data 消息 JSON 载荷。
     * @return 解析成功返回新消息对象，失败返回 {@code null}。
     */
    @Override
    public CPMessageData parse(JsonNode data) {
        if (data == null || !data.has("text") || !data.get("text").isTextual()) {
            return null;
        }
        String text = data.get("text").asText();
        String shortContent = text.length() <= 20 ? text : text.substring(0, 20);
        return new CPTextMessage(shortContent, data);
    }

    /**
     * 获取可检索摘要文本。
     *
     * @return 文本消息摘要。
     */
    @Override
    public String getSContent() {
        return sContent;
    }

    /**
     * 获取原始消息数据。
     *
     * @return 消息 JSON 载荷。
     */
    @Override
    public JsonNode getData() {
        return data;
    }

    /**
     * 获取消息领域。
     *
     * @return 固定返回 `Core:Text`。
     */
    @Override
    public String getDomain() {
        return DOMAIN;
    }
}
