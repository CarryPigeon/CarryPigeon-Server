package team.carrypigeon.backend.chat.domain.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.chat.domain.manager.user.CPUserToChannelManager;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

import java.util.Map;

/**
 * controller分发器，用于对请求根据路径分发到具体的处理器处理
 * */
@Component
@Slf4j
public class CPControllerDispatcherImpl implements CPControllerDispatcher {

    private final ObjectMapper mapper;

    private final Map<String, CPController> controllers;

    private final CPUserToChannelManager cpUserManager;

    public CPControllerDispatcherImpl(ObjectMapper mapper, Map<String, CPController> controllers, CPUserToChannelManager cpUserManager) {
        this.mapper = mapper;
        this.controllers = controllers;
        this.cpUserManager = cpUserManager;
    }

    @Override
    public CPResponse process(String msg, CPChannel channel) {
        try {
            CPPacket route = mapper.readValue(msg, CPPacket.class);
            if (!controllers.containsKey(route.getRoute())){
                return new CPResponse(route.getId(),404,mapper.convertValue(Map.of("msg","no such route"), JsonNode.class));
            }
            CPResponse response = controllers.get(route.getRoute()).process(route.getData(),channel);
            if (response == null) return null;
            response.setId(route.getId());
            return  response;
        } catch (JsonProcessingException e) {
            log.error("json处理错误，json字符串：{}",msg);
            log.error(e.getMessage(),e);
        }
        return CPResponse.ERROR_RESPONSE.copy();
    }

    @Override
    public void channelInactive(CPChannel cpChannel) {
        cpUserManager.removeChannel(cpChannel);

    }
}
