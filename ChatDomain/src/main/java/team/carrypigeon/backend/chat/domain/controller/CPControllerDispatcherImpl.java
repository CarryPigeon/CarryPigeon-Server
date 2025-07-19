package team.carrypigeon.backend.chat.domain.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.chat.domain.manager.user.CPUserManager;
import team.carrypigeon.backend.chat.domain.pojo.controller.CPRoute;
import team.carrypigeon.backend.common.response.CPResponse;

import java.util.Map;

@Component
@Slf4j
public class CPControllerDispatcherImpl implements CPControllerDispatcher {

    private final ObjectMapper mapper;

    private final Map<String, CPController> controllers;

    private final CPUserManager cpUserManager;

    public CPControllerDispatcherImpl(ObjectMapper mapper, Map<String, CPController> controllers, CPUserManager cpUserManager) {
        this.mapper = mapper;
        this.controllers = controllers;
        this.cpUserManager = cpUserManager;
    }

    @Override
    public CPResponse process(String msg, CPChannel channel) {
        try {
            CPRoute route = mapper.readValue(msg, CPRoute.class);
            if (!controllers.containsKey(route.getRoute())){
                return new CPResponse(route.getId(),404,mapper.convertValue(Map.of("msg","no such route"), JsonNode.class));
            }
            CPController controller = controllers.get(route.getRoute());
            CPResponse response = controller.process(route.getData(),channel);
            response.setId(route.getId());
            return  response;
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        CPResponse response = new CPResponse();
        response.setCode(100);
        return  response;
    }

    @Override
    public void channelInactive(CPChannel cpChannel) {
        cpUserManager.removeChannel(cpChannel);
    }
}
