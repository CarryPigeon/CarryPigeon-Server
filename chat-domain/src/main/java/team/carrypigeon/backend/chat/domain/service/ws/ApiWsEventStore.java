package team.carrypigeon.backend.chat.domain.service.ws;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * WS 事件内存存储（用于 resume 回放窗口）。
 * <p>
 * 特性：
 * <ul>
 *   <li>按 {@code event_id}（雪花 ID）递增存储</li>
 *   <li>提供 {@code last_event_id} 之后的事件回放</li>
 *   <li>按 TTL 与容量上限淘汰</li>
 * </ul>
 * <p>
 * 注意：该存储为内存实现，服务端重启会清空；客户端需在 resume 失败时走 HTTP 补拉。
 */
@Slf4j
@Service
public class ApiWsEventStore {

    private final CpApiProperties properties;
    private final NavigableMap<Long, StoredEvent> events = new ConcurrentSkipListMap<>();

    /**
     * 构造 WS 事件存储。
     *
     * @param properties API 配置
     */
    public ApiWsEventStore(CpApiProperties properties) {
        this.properties = properties;
    }

    /**
     * 追加一条事件并触发淘汰。
     */
    public void append(StoredEvent e) {
        if (e == null) {
            return;
        }
        events.put(e.eventIdLong(), e);
        evictIfNeeded();
    }

    /**
     * 从 {@code last_event_id} 之后回放事件。
     *
     * @param lastEventId 客户端最后已处理的 event_id（字符串形式的雪花 ID）
     * @param maxEvents   单次最多回放条数（避免巨量补发）
     */
    public ResumeResult resumeAfter(String lastEventId, int maxEvents) {
        long last = parseLongOrZero(lastEventId);
        if (last <= 0) {
            return new ResumeResult(List.of(), null);
        }
        if (events.isEmpty()) {
            return new ResumeResult(List.of(), null);
        }
        Long earliest = events.firstKey();
        if (earliest != null && last < earliest) {
            return new ResumeResult(List.of(), CPProblemReason.EVENT_TOO_OLD.code());
        }
        List<StoredEvent> out = new ArrayList<>();
        for (StoredEvent e : events.tailMap(last, false).values()) {
            out.add(e);
            if (out.size() >= maxEvents) {
                break;
            }
        }
        return new ResumeResult(out, null);
    }

    /**
     * 按 TTL 与 capacity 进行淘汰。
     */
    private void evictIfNeeded() {
        int capacity = properties.getApi().getWs().getEventStore().getCapacity();
        int ttlSeconds = properties.getApi().getWs().getEventStore().getTtlSeconds();
        long ttlMillis = Math.max(1, ttlSeconds) * 1000L;
        long now = System.currentTimeMillis();
        while (!events.isEmpty()) {
            StoredEvent first = events.firstEntry().getValue();
            if (first == null) {
                events.pollFirstEntry();
                continue;
            }
            if (now - first.serverTime() <= ttlMillis) {
                break;
            }
            events.pollFirstEntry();
        }
        while (events.size() > capacity && !events.isEmpty()) {
            events.pollFirstEntry();
        }
    }

    /**
     * 将字符串解析为 long；失败返回 0。
     */
    private long parseLongOrZero(String s) {
        if (s == null || s.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public record StoredEvent(String eventId,
                             long eventIdLong,
                             String eventType,
                             long serverTime,
                             JsonNode payload) {
        public StoredEvent {
            Objects.requireNonNull(eventId, "eventId");
            Objects.requireNonNull(eventType, "eventType");
        }
    }

    /**
     * resume 结果。
     *
     * @param events       回放事件列表
     * @param failedReason 失败原因；非空表示无法回放（例如 event_too_old）
     */
    public record ResumeResult(List<StoredEvent> events, String failedReason) {
    }
}
