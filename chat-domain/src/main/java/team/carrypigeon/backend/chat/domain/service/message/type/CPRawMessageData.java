package team.carrypigeon.backend.chat.domain.service.message.type;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;

/**
 * 插件领域原始消息对象。
 * <p>
 * 用于承载非 `Core:*` 领域的原始 JSON 数据。
 */
public class CPRawMessageData implements CPMessageData {

    private final String domain;
    private final JsonNode data;

    /**
     * 构造原始消息对象。
     *
     * @param domain 消息领域。
     * @param data 消息载荷。
     */
    public CPRawMessageData(String domain, JsonNode data) {
        this.domain = domain;
        this.data = data;
    }

    /**
     * 解析消息数据。
     *
     * @param data 消息 JSON 载荷。
     * @return 新的原始消息对象。
     */
    @Override
    public CPMessageData parse(JsonNode data) {
        return new CPRawMessageData(domain, data);
    }

    /**
     * 获取可检索摘要文本。
     *
     * @return 插件原始消息默认返回空字符串。
     */
    @Override
    public String getSContent() {
        return "";
    }

    /**
     * 获取消息原始数据。
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
     * @return 消息领域标识。
     */
    @Override
    public String getDomain() {
        return domain;
    }
}
