package team.carrypigeon.backend.chat.domain.service.message.type;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageDomain;

/**
 * 文本消息<br/>
 * 对应的json结构<br/>
 *
 * ```json
 * {
 *     "text":""
 * }
 * ```
 * */
@CPMessageDomain("Core:Text")
public class CPTextMessage implements CPMessageData {

    @Override
    public CPMessageData parse(JsonNode data) {
        return null;
    }

    @Override
    public String getSContent() {
        return "";
    }

    @Override
    public JsonNode getData() {
        return null;
    }

    @Override
    public String getDomain() {
        return "";
    }


}
