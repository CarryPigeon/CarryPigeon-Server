package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

import java.util.HashMap;
import java.util.Map;

import static team.carrypigeon.backend.api.connection.protocol.CPResponse.SUCCESS_RESPONSE;

/**
 * 通用控制器抽象基类。
 * <p>
 * 负责：
 * <ul>
 *     <li>将请求 JSON 反序列化为 VO 对象；</li>
 *     <li>执行业务前的参数校验 {@link #check(CPSession, Object, Map)}；</li>
 *     <li>执行业务主体 {@link #process0(CPSession, Object, Map)}；</li>
 *     <li>在成功时触发通知 {@link #notify(CPSession, Object, Map)}。</li>
 * </ul>
 * 子类只需关注具体的 VO 类型及业务实现。
 */
public abstract class CPControllerAbstract<T> implements CPController {

    protected final ObjectMapper objectMapper;

    protected final Class<T> VOClazz;

    public CPControllerAbstract(ObjectMapper objectMapper, Class<T> clazz) {
        this.objectMapper = objectMapper;
        this.VOClazz = clazz;
    }

    @Override
    public CPResponse process(CPSession session, JsonNode data) {
        // 构造上下文，供后续节点协同使用
        Map<String, Object> context = new HashMap<>();
        T vo;
        try {
            // 反序列化请求数据为 VO
            vo = objectMapper.treeToValue(data, VOClazz);
        } catch (JsonProcessingException e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 执行参数校验
        CPResponse checkResponse = check(session, vo, context);
        if (checkResponse != null) {
            return checkResponse;
        }
        // 执行业务逻辑
        CPResponse cpResponse = process0(session, vo, context);
        // 成功时触发通知逻辑
        if (cpResponse.getCode() == SUCCESS_RESPONSE.getCode()) {
            notify(session, vo, context);
        }
        return cpResponse;
    }

    /**
     * 参数检查阶段。
     *
     * @param session 当前会话
     * @param data    反序列化后的 VO 数据
     * @param context 业务上下文，供后续流程共用
     * @return {@code null} 表示校验通过；非空则直接返回该响应并中断流程
     */
    protected abstract CPResponse check(CPSession session, T data, Map<String, Object> context);

    /**
     * 业务处理阶段。
     *
     * @param session 当前会话
     * @param data    反序列化后的 VO 数据
     * @param context 业务上下文，供后续流程共用
     * @return 业务执行后的响应
     */
    protected abstract CPResponse process0(CPSession session, T data, Map<String, Object> context);

    /**
     * 通知阶段，在业务成功后调用，用于发送消息、推送等副作用逻辑。
     *
     * @param session 当前会话
     * @param data    反序列化后的 VO 数据
     * @param context 业务上下文
     */
    protected void notify(CPSession session, T data, Map<String, Object> context) {
        // 默认不做任何事情，子类按需重写
    }
}