package team.carrypigeon.backend.chat.domain.controller.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.connection.protocol.CPPacket;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;

import java.util.Map;

/**
 * controller分发器，用于对请求根据路径分发到具体的处理器处理
 * */
@Component
@Slf4j
public class CPControllerDispatcherImpl implements CPControllerDispatcher {

    private final ObjectMapper mapper;
    private final Map<String, Class<?>> controllerAndVOMap;
    private final CPSessionCenterService cpSessionCenterService;
    private final FlowExecutor flowExecutor;

    public CPControllerDispatcherImpl(ObjectMapper mapper, Map<String, Class<?>> controllerAndVOMap, CPSessionCenterService cpSessionCenterService, FlowExecutor flowExecutor) {
        this.mapper = mapper;
        this.controllerAndVOMap = controllerAndVOMap;
        this.cpSessionCenterService = cpSessionCenterService;
        this.flowExecutor = flowExecutor;
    }

    @Override
    public CPResponse process(String msg, CPSession session) {
        try {
            CPPacket route = mapper.readValue(msg, CPPacket.class);
            // 新建一个默认上下文
            DefaultContext defaultContext = new DefaultContext();
            Class<?> aClass = controllerAndVOMap.get(route.getRoute());
            if (aClass == null){
                return CPResponse.PATH_NOT_FOUND_RESPONSE.copy();
            }
            CPControllerVO vo = (CPControllerVO)mapper.treeToValue(route.getData(), aClass);
            // 放入数据
            if (!vo.insertData(defaultContext)) {
                return CPResponse.ERROR_RESPONSE.copy().setTextData("error args");
            }
            LiteflowResponse liteflowResponse = flowExecutor.execute2Resp(route.getRoute(), null, defaultContext);
            CPResponse response = liteflowResponse.getContextBean(DefaultContext.class).getData("response");
            if (response == null){
                response = CPResponse.SUCCESS_RESPONSE.copy();
            }
            response.setId(route.getId());
            return  response;
        } catch (JsonProcessingException e) {
            log.error("json处理错误，json字符串：{}",msg);
            log.error(e.getMessage(),e);
        }
        return CPResponse.ERROR_RESPONSE.copy();
    }

    @Override
    public void channelInactive(CPSession session) {
        Long attributeValue = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        if (attributeValue != null){
            cpSessionCenterService.removeSession(attributeValue,session);
        }
    }
}
