package team.carrypigeon.backend.chat.domain.service.message.type;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageDomain;

/**
 * 文本消息的解析与封装实现。<br/>
 * <p>
 * 对应的 JSON 结构约定为：
 * <pre>
 * {
 *   "text": "消息内容"
 * }
 * </pre>
 * 插件只需保证该结构，具体长度限制由业务层自行控制。
 */
@CPMessageDomain("Core:Text")
public class CPTextMessage implements CPMessageData {

    private static final String DOMAIN = "Core:Text";

    /**
     * 用于通知和列表展示的简要内容（通常为 text 的前若干字符）。
     */
    private String sContent;

    /**
     * 原始消息数据。
     */
    private JsonNode data;

    public CPTextMessage() {
    }

    public CPTextMessage(String sContent, JsonNode data) {
        this.sContent = sContent;
        this.data = data;
    }

    /**
     * 解析文本消息数据。<br/>
     * <p>
     * 规则：
     * <ul>
     *     <li>data 必须为对象且包含 text 字段</li>
     *     <li>text 字段必须为字符串</li>
     *     <li>sContent 取 text 的前 20 个字符（不足则全部）</li>
     * </ul>
     *
     * @param data 原始 JSON 数据
     * @return 解析成功返回新的 CPTextMessage 实例，失败返回 null
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

    @Override
    public String getSContent() {
        return sContent;
    }

    @Override
    public JsonNode getData() {
        return data;
    }

    @Override
    public String getDomain() {
        return DOMAIN;
    }
}
