package team.carrypigeon.backend.chat.domain.service.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;

import java.util.Map;

/**
 * 消息解析服务：根据消息域（domain）将 JSON data 解析为具体的 {@link CPMessageData}。
 */
@Slf4j
@Service
public class CPMessageParserService {
    private final Map<String, CPMessageData> messageDataMap;

    /**
     * 创建消息解析服务（由 Spring 注入所有已注册的 {@link CPMessageData}）。
     */
    public CPMessageParserService(Map<String, CPMessageData> messageDataMap) {
        this.messageDataMap = messageDataMap;
    }

    /**
     * 解析消息数据
     * @param domain 消息域
     * @param data 消息数据
     * @return 解析后的消息数据，null则为解析失败或不支持的类型
     */
    public CPMessageData parse(String domain, JsonNode data) {
        if (!messageDataMap.containsKey(domain)) {
            log.warn("CPMessageParserService: unsupported domain {}, rawData={}", domain, data);
            return null;
        }
        CPMessageData parser = messageDataMap.get(domain);
        return parser.parse(data);
    }

    public boolean supports(String domain) {
        return domain != null && messageDataMap.containsKey(domain);
    }
}
