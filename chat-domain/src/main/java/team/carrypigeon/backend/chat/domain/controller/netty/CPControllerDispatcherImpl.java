package team.carrypigeon.backend.chat.domain.controller.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.connection.protocol.CPPacket;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller 分发器，根据路由将请求分发到具体的处理器进行处理。
 */
@Component
@Slf4j
public class CPControllerDispatcherImpl implements CPControllerDispatcher {

    private final Map<String, Class<?>> controllerAndVOMap;
    private final Map<String, Class<?>> controllerAndResultMap;
    private final ObjectMapper mapper;
    private final CPSessionCenterService cpSessionCenterService;
    private final FlowExecutor flowExecutor;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    public CPControllerDispatcherImpl(ObjectMapper mapper,
                                      CPSessionCenterService cpSessionCenterService,
                                      FlowExecutor flowExecutor,
                                      ObjectMapper objectMapper,
                                      ApplicationContext applicationContext) {
        this.mapper = mapper;
        this.cpSessionCenterService = cpSessionCenterService;
        this.flowExecutor = flowExecutor;
        this.controllerAndVOMap = new HashMap<>();
        this.controllerAndResultMap = new HashMap<>();
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
        initControllerMapsIfNecessary();
    }

    /**
     * 初始化 controller 与 VO/Result 的映射关系。<br/>
     * 如果 CPControllerPostProcessor 已经填充过 map，则这里不会覆盖；<br/>
     * 否则会基于 {@link CPControllerTag} 注解进行一次扫描补充。
     */
    private void initControllerMapsIfNecessary() {
        if (!controllerAndVOMap.isEmpty() && !controllerAndResultMap.isEmpty()) {
            return;
        }
        Map<String, Object> beansWithTag = applicationContext.getBeansWithAnnotation(CPControllerTag.class);
        for (Object bean : beansWithTag.values()) {
            Class<?> beanClass = bean.getClass();
            CPControllerTag annotation = beanClass.getAnnotation(CPControllerTag.class);
            if (annotation == null) {
                continue;
            }
            Class<?> voClazz = annotation.voClazz();
            Class<?> resultClazz = annotation.resultClazz();
            if (!CPControllerVO.class.isAssignableFrom(voClazz)) {
                log.warn("CPControllerTag on {} has voClazz {} which does not implement CPControllerVO",
                        beanClass.getName(), voClazz.getName());
                continue;
            }
            if (!CPControllerResult.class.isAssignableFrom(resultClazz)) {
                log.warn("CPControllerTag on {} has resultClazz {} which does not implement CPControllerResult",
                        beanClass.getName(), resultClazz.getName());
                continue;
            }
            String path = annotation.path();
            controllerAndVOMap.putIfAbsent(path, voClazz);
            controllerAndResultMap.putIfAbsent(path, resultClazz);
            log.info("register controller path:{}, vo:{}, result:{}", path, voClazz.getName(), resultClazz.getName());
        }
    }

    @Override
    public CPResponse process(String msg, CPSession session) {
        long startTime = System.currentTimeMillis();
        try {
            CPPacket route = mapper.readValue(msg, CPPacket.class);
            // 将关键上下文写入 MDC，便于在整条日志链路中追踪
            MDC.put("packetId", String.valueOf(route.getId()));
            MDC.put("route", route.getRoute());
            Long uidAttr = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
            if (uidAttr != null) {
                MDC.put("uid", String.valueOf(uidAttr));
            }
            log.debug("receive packet, id={}, route={}", route.getId(), route.getRoute());
            // 新建一个默认上下文
            DefaultContext defaultContext = new DefaultContext();
            // 将 session 写入上下文，供各个 CPNodeComponent 使用
            defaultContext.setData(CPNodeValueKeyBasicConstants.SESSION, session);

            Class<?> voClazz = controllerAndVOMap.get(route.getRoute());
            if (voClazz == null) {
                log.warn("route not found for path {}, id={}", route.getRoute(), route.getId());
                return CPResponse.PATH_NOT_FOUND_RESPONSE.copy();
            }
            CPControllerVO vo = (CPControllerVO) mapper.treeToValue(route.getData(), voClazz);
            // 前置处理，将请求参数写入 context
            if (!vo.insertData(defaultContext)) {
                log.warn("insertData returned false, route={}, id={}", route.getRoute(), route.getId());
                return CPResponse.ERROR_RESPONSE.copy().setTextData("error args");
            }

            LiteflowResponse liteflowResponse = flowExecutor.execute2Resp(route.getRoute(), null, defaultContext);
            if (!liteflowResponse.isSuccess()) {
                log.error("liteflow chain execute failed, route={}, id={}, message={}",
                        route.getRoute(), route.getId(), liteflowResponse.getMessage());
            } else {
                log.debug("liteflow chain execute success, route={}, id={}", route.getRoute(), route.getId());
            }

            Class<?> resultClazz = controllerAndResultMap.get(route.getRoute());
            if (resultClazz == null) {
                log.warn("result class not found for path {}, id={}", route.getRoute(), route.getId());
                return CPResponse.PATH_NOT_FOUND_RESPONSE.copy();
            }

            CPControllerResult result;
            try {
                result = (CPControllerResult) resultClazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("failed to construct CPControllerResult for route {}", route.getRoute(), e);
                return CPResponse.SERVER_ERROR.copy().setId(route.getId());
            }
            // 后置处理，组装 response
            result.process(session, defaultContext, objectMapper);

            CPResponse response = liteflowResponse.getContextBean(DefaultContext.class)
                    .getData(CPNodeValueKeyBasicConstants.RESPONSE);
            if (response == null) {
                response = CPResponse.SUCCESS_RESPONSE.copy();
            }
            response.setId(route.getId());
            long cost = System.currentTimeMillis() - startTime;
            log.debug("return response, id={}, route={}, code={}, cost={}ms",
                    route.getId(), route.getRoute(), response.getCode(), cost);
            if (cost > 500) {
                log.warn("slow request detected, id={}, route={}, cost={}ms",
                        route.getId(), route.getRoute(), cost);
            }
            return response;
        } catch (JsonProcessingException e) {
            // JSON 解析失败，记录原始报文
            long cost = System.currentTimeMillis() - startTime;
            log.error("json 处理错误，json 字符串={}，cost={}ms", msg, cost);
            log.error(e.getMessage(), e);
        } finally {
            // 清理 MDC，避免数据泄露到后续请求
            MDC.clear();
        }
        return CPResponse.ERROR_RESPONSE.copy();
    }

    @Override
    public void channelInactive(CPSession session) {
        Long attributeValue = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        if (attributeValue != null) {
            cpSessionCenterService.removeSession(attributeValue, session);
            log.info("session closed, uid={}", attributeValue);
        } else {
            log.debug("session closed without CHAT_DOMAIN_USER_ID attribute");
        }
    }
}
