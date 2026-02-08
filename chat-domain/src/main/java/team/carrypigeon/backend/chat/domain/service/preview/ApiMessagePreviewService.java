package team.carrypigeon.backend.chat.domain.service.preview;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

/**
 * 消息预览（preview）生成服务。
 * <p>
 * 设计目标：
 * <ul>
 *   <li>支持“未安装插件/未知 domain”的降级展示</li>
 *   <li>避免泄露非 {@code Core:*} domain 的 payload 全量数据</li>
 *   <li>保证 preview 短、稳定、可缓存</li>
 * </ul>
 * <p>
 * 约束（与 PRD 对齐）：
 * <ul>
 *   <li>{@code Core:Text}：从 data.text 截断生成</li>
 *   <li>非 {@code Core:*}：不读取 payload 字段，统一输出 {@code [domain]}</li>
 * </ul>
 */
@Service
public class ApiMessagePreviewService {

    /**
     * {@code Core:Text} 的 preview 最大长度。
     */
    private static final int CORE_TEXT_MAX = 20;

    /**
     * 生成 preview。
     *
     * @param domain 消息 domain（例如 Core:Text）
     * @param data   消息 payload
     * @return 安全且可展示的 preview
     */
    public String preview(String domain, JsonNode data) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        if ("Core:Text".equals(domain)) {
            return coreTextPreview(data);
        }

        // For non-core domains, do not expose payload fields. Use a stable marker for degrade display.
        return "[" + domain + "]";
    }

    /**
     * 生成 {@code Core:Text} 的 preview。
     */
    private String coreTextPreview(JsonNode data) {
        if (data == null) {
            return "";
        }
        JsonNode textNode = data.get("text");
        if (textNode == null || !textNode.isTextual()) {
            return "";
        }
        return normalize(textNode.asText(), CORE_TEXT_MAX);
    }

    /**
     * 对字符串进行“可展示化”处理：去除换行并截断长度。
     */
    private String normalize(String s, int max) {
        if (s == null) {
            return "";
        }
        String out = s.replace('\n', ' ').replace('\r', ' ').trim();
        if (out.length() <= max) {
            return out;
        }
        return out.substring(0, max);
    }
}
