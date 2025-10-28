package team.carrypigeon.backend.api.chat.domain.message;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * cp的消息数据接口，用于封装不同的消息数据
 * */
public interface CPMessageData {
    /**
     * 用于解析消息数据并封装
     * */
    CPMessageData parse(JsonNode data);

    /**
     * 获取简略消息，主要用于消息向客户端的推送<br/>
     * 例如：文本消息的简略消息为前20个字符，图片的简略信息为[image]
     * */
    String getSContent();

    /**
     * 获取消息数据
     * */
    JsonNode getData();

    /**
     * 获取消息域
     * */
    String getDomain();
}