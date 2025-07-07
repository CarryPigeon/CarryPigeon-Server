package team.carrypigeon.backend.chat.domain.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.chat.domain.pojo.controller.CPRoute;
import team.carrypigeon.backend.common.response.CPResponse;

import java.util.Map;

@Component
@Slf4j
public class CPControllerDispatcherImpl implements CPControllerDispatcher {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Map<String, CPController> controllers;

    @Override
    public CPResponse process(String msg) {
        // TODO 判断是否进行了密钥交换
        // TODO 判断是否登录
        // 进行路径分配
        try {
            CPRoute route = mapper.readValue(msg, CPRoute.class);
            if (!controllers.containsKey(route.getRoute())){
                return new CPResponse(route.getId(),404,mapper.convertValue(Map.of("msg","no such route"), JsonNode.class));
            }
            CPController controller = controllers.get(route.getRoute());
            CPResponse response = controller.process(route.getData());
            response.setId(route.getId());
            return  response;
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        // TODO 异常处理
        return null;
    }
}
