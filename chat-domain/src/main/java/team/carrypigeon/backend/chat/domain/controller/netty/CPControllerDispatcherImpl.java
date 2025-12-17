package team.carrypigeon.backend.chat.domain.controller.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.connection.CPConnectionAttributes;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowConnectionInfo;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPPacket;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.flow.FlowTxExecutor;
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
    private final FlowTxExecutor flowTxExecutor;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    public CPControllerDispatcherImpl(ObjectMapper mapper,
                                      CPSessionCenterService cpSessionCenterService,
                                      FlowTxExecutor flowTxExecutor,
                                      ObjectMapper objectMapper,
                                      ApplicationContext applicationContext) {
        this.mapper = mapper;
        this.cpSessionCenterService = cpSessionCenterService;
        this.flowTxExecutor = flowTxExecutor;
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
            // 新建一个带链路查询缓存的上下文
            CPFlowContext context = new CPFlowContext();
            // 将连接信息写入上下文，供各个 Node 做限流等控制
            context.setConnectionInfo(buildConnectionInfo(session));
            // 将 session 写入上下文，供各个 CPNodeComponent 使用
            context.setData(CPNodeCommonKeys.SESSION, session);

            Class<?> voClazz = controllerAndVOMap.get(route.getRoute());
            if (voClazz == null) {
                log.warn("route not found for path {}, id={}", route.getRoute(), route.getId());
                return CPResponse.pathNotFound();
            }
            CPControllerVO vo = (CPControllerVO) mapper.treeToValue(route.getData(), voClazz);
            // 前置处理，将请求参数写入 context
            if (!vo.insertData(context)) {
                log.warn("insertData returned false (invalid request args), route={}, id={}", route.getRoute(), route.getId());
                return CPResponse.error("invalid request args");
            }

            LiteflowResponse liteflowResponse = flowTxExecutor.executeWithTx(route.getRoute(), context);
            if (!liteflowResponse.isSuccess()) {
                log.error("liteflow chain execute failed, route={}, id={}, message={}",
                        route.getRoute(), route.getId(), liteflowResponse.getMessage());
            } else {
                log.debug("liteflow chain execute success, route={}, id={}", route.getRoute(), route.getId());
            }

            Class<?> resultClazz = controllerAndResultMap.get(route.getRoute());
            if (resultClazz == null) {
                log.warn("result class not found for path {}, id={}", route.getRoute(), route.getId());
                return CPResponse.pathNotFound();
            }

            CPControllerResult result;
            try {
                result = (CPControllerResult) resultClazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("failed to construct CPControllerResult for route {}", route.getRoute(), e);
                return CPResponse.serverError().setId(route.getId());
            }
            // 后置处理，组装 response
            result.process(session, context, objectMapper);

            CPResponse response = liteflowResponse.getContextBean(CPFlowContext.class)
                    .getData(CPNodeCommonKeys.RESPONSE);
            if (response == null) {
                response = CPResponse.success();
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
        return CPResponse.error();
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

    /**
     * 从 {@link CPSession} 提取连接相关信息，构造 {@link CPFlowConnectionInfo}。
     * <p>
     * 如果底层未写入对应属性，则返回的字段可能为 null。
     */
    private CPFlowConnectionInfo buildConnectionInfo(CPSession session) {
        if (session == null) {
            return null;
        }
        String remoteAddress = session.getAttributeValue(CPConnectionAttributes.REMOTE_ADDRESS, String.class);
        String remoteIp = session.getAttributeValue(CPConnectionAttributes.REMOTE_IP, String.class);
        Integer remotePort = session.getAttributeValue(CPConnectionAttributes.REMOTE_PORT, Integer.class);
        if (remoteAddress == null && remoteIp == null && remotePort == null) {
            return null;
        }
        return new CPFlowConnectionInfo()
                .setRemoteAddress(remoteAddress)
                .setRemoteIp(remoteIp)
                .setRemotePort(remotePort);
    }
}
