package team.carrypigeon.backend.chat.domain.service.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;

import java.util.Map;

/**
 * 消息解析服务。
 * <p>
 * 根据消息领域选择对应解析器，将 JSON 载荷转换为 `CPMessageData`。
 */
@Slf4j
@Service
public class CPMessageParserService {

    private final Map<String, CPMessageData> messageDataMap;

    /**
     * 构造消息解析服务。
     *
     * @param messageDataMap 按领域名注册的消息解析器映射。
     */
    public CPMessageParserService(Map<String, CPMessageData> messageDataMap) {
        this.messageDataMap = messageDataMap;
    }

    /**
     * 解析指定领域消息。
     *
     * @param domain 消息领域。
     * @param data 消息 JSON 载荷。
     * @return 解析后的消息对象；领域不支持或解析失败时返回 {@code null}。
     */
    public CPMessageData parse(String domain, JsonNode data) {
        if (!messageDataMap.containsKey(domain)) {
            log.warn("CPMessageParserService: unsupported domain {}, rawData={}", domain, data);
            return null;
        }
        CPMessageData parser = messageDataMap.get(domain);
        return parser.parse(data);
    }

    /**
     * 判断是否支持指定领域。
     *
     * @param domain 消息领域标识。
     * @return 支持返回 {@code true}，否则返回 {@code false}。
     */
    public boolean supports(String domain) {
        return domain != null && messageDataMap.containsKey(domain);
    }
}
