package team.carrypigeon.backend.chat.domain.service.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WS 会话注册表（uid -> sessions）。
 * <p>
 * 仅记录“已鉴权”的 WS 会话，用于事件推送：
 * <ul>
 *   <li>{@link #register(long, WebSocketSession)}：鉴权成功后绑定 uid</li>
 *   <li>{@link #unregister(WebSocketSession)}：连接关闭或 reauth 重新绑定时解绑</li>
 * </ul>
 */
@Slf4j
@Service
public class ApiWsSessionRegistry {

    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> sessionsByUid = new ConcurrentHashMap<>();
    private final Map<String, Long> uidBySessionId = new ConcurrentHashMap<>();

    /**
     * 绑定会话到 uid（同 uid 可有多个会话）。
     */
    public void register(long uid, WebSocketSession session) {
        uidBySessionId.put(session.getId(), uid);
        sessionsByUid.computeIfAbsent(uid, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("WS register, uid={}, sessionId={}", uid, session.getId());
    }

    /**
     * 从注册表移除会话绑定关系。
     */
    public void unregister(WebSocketSession session) {
        if (session == null) {
            return;
        }
        Long uid = uidBySessionId.remove(session.getId());
        if (uid == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> set = sessionsByUid.get(uid);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessionsByUid.remove(uid);
            }
        }
        log.debug("WS unregister, uid={}, sessionId={}", uid, session.getId());
    }

    /**
     * 获取 uid 的所有在线会话（若无则返回空集合）。
     */
    public Collection<WebSocketSession> sessionsOf(long uid) {
        Set<WebSocketSession> s = sessionsByUid.get(uid);
        return s == null ? Collections.emptySet() : s;
    }
}
