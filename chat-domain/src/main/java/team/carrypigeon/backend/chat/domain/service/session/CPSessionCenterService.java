package team.carrypigeon.backend.chat.domain.service.session;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.connection.CPSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * carrypigeon聊天会话中心服务，用于存储用户id与会话的映射关系
 * @author midreamsheep
 * */
@Service
public class CPSessionCenterService {
    private final Map<Long, List<CPSession>> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 添加一个会话
     * @param uid 用户id
     * @param session 会话
     * */
    public void addSession(long uid, CPSession session){
        if (session == null) {
            return;
        }
        if (!SESSION_MAP.containsKey(uid)){
            List<CPSession> sessions = new CopyOnWriteArrayList<>();
            sessions.add(session);
            SESSION_MAP.put(uid, sessions);
        }else {
            SESSION_MAP.get(uid).add(session);
        }
    }

    /**
     * 移除一个会话
     * @param uid 用户id
     * @param session 会话
     * */
    public void removeSession(long uid, CPSession session){
        SESSION_MAP.computeIfPresent(uid, (k, v) -> {
            v.remove(session);
            return v;
        });
    }

    /**
     * 获取用户id对应的所有会话
     * @param uid 用户id
     * @return 用户id对应的所有会话
     * */
    public List<CPSession> getSessions(long uid){
        return SESSION_MAP.get(uid);
    }

    /**
     * 定时任务，每天凌晨3点清理空SESSION组
     * */
    @Scheduled(cron = "0 0 3 * * ?")
    public void clean() {
        SESSION_MAP.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}