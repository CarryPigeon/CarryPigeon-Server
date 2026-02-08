package team.carrypigeon.backend.api.connection.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 服务端推送通知体（作为 {@code CPResponse(id=-1, code=0)} 的 data）。
 * <p>
 * 推送协议外层统一是 {@code CPResponse}，其 {@code data} 字段为该对象：
 * <pre>
 * {
 *   "id": -1,
 *   "code": 0,
 *   "data": { "route": "...", "data": { ... } }
 * }
 * </pre>
 *
 * <p>其中：
 * <ul>
 *     <li>{@link #route}：通知类型（例如 {@code "/core/message"}、{@code "handshake"}）。</li>
 *     <li>{@link #data}：通知 payload（结构由 route 定义）。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPNotification {
    private String route;
    private JsonNode data;
}
