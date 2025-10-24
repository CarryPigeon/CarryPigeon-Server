package team.carrypigeon.backend.chat.domain.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
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
public class CPNotificationService {

    private final CPSessionCenterService cpSessionCenterService;

    private final ObjectMapper objectMapper;

    public CPNotificationService(CPSessionCenterService cpSessionCenterService, ObjectMapper objectMapper) {
        this.cpSessionCenterService = cpSessionCenterService;
        this.objectMapper = objectMapper;
    }

    public boolean sendNotification(Collection<Long> uids, CPNotification notification) {
        for (long uid : uids) {
            List<CPSession> sessions = cpSessionCenterService.getSessions(uid);
            if (sessions == null) continue;
            for (CPSession session : sessions) {
                CPResponse cpResponse = new CPResponse();
                cpResponse.setId(-1)
                        .setCode(0)
                        .setData(objectMapper.valueToTree( notification));
                try {
                    session.write(objectMapper.writeValueAsString(cpResponse));
                } catch (JsonProcessingException e) {
                    log.error("发送通知失败");
                }
            }
        }
        return true;
    }
}
