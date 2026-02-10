package team.carrypigeon.backend.chat.domain.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.notification.CPNotificationSender;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;

import java.util.Collection;
import java.util.List;


/**
 * Carrypigeon通知服务
 * 封装通知服务，提供通知服务
 * @author midreamsheep
 * */
@Slf4j
@Service
public class CPNotificationService implements CPNotificationSender {

    private final CPSessionCenterService cpSessionCenterService;

    private final ObjectMapper objectMapper;

    /**
     * 创建通知服务（由 Spring 注入会话中心与 {@link ObjectMapper}）。
     */
    public CPNotificationService(CPSessionCenterService cpSessionCenterService, ObjectMapper objectMapper) {
        this.cpSessionCenterService = cpSessionCenterService;
        this.objectMapper = objectMapper;
    }

    /**
     * 向目标用户集合发送通知消息。
     *
     * @param uids 目标用户 ID 集合
     * @param notification 待发送的通知体
     * @return {@code true} 表示通知分发流程执行完成（单用户失败仅记录日志）
     */
    @Override
    public boolean sendNotification(Collection<Long> uids, CPNotification notification) {
        if (uids == null || uids.isEmpty()) {
            log.debug("sendNotification called with empty uids, notificationType={}",
                    notification == null ? null : notification.getClass().getSimpleName());
            return true;
        }
        for (long uid : uids) {
            List<CPSession> sessions = cpSessionCenterService.getSessions(uid);
            if (sessions == null || sessions.isEmpty()) {
                log.debug("sendNotification skip, no active session, uid={}", uid);
                continue;
            }
            for (CPSession session : sessions) {
                CPResponse cpResponse = CPResponse.notification(objectMapper.valueToTree(notification));
                try {
                    session.write(objectMapper.writeValueAsString(cpResponse));
                } catch (JsonProcessingException e) {
                    log.error("sendNotification failed, uid={}", uid, e);
                }
            }
        }
        return true;
    }
}
