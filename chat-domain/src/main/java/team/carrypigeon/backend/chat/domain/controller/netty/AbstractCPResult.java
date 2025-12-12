package team.carrypigeon.backend.chat.domain.controller.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

/**
 * Netty 结果处理抽象基类。
 * <p>
 * 封装向 LiteFlow {@link DefaultContext} 写入 {@link CPResponse} 的通用逻辑，
 * 具体业务结果类只需要关心从上下文读取数据并构造响应体。
 */
public abstract class AbstractCPResult implements CPControllerResult {

    /**
     * 写入成功响应，可选择携带 JSON 响应体。
     */
    protected void writeSuccess(DefaultContext context, ObjectMapper objectMapper, Object body) {
        CPResponse response = CPResponse.SUCCESS_RESPONSE.copy();
        if (body != null) {
            response.setData(objectMapper.valueToTree(body));
        }
        context.setData(CPNodeCommonKeys.RESPONSE, response);
    }

    /**
     * 写入携带纯文本消息的错误响应。
     */
    protected void writeError(DefaultContext context, String message) {
        CPResponse response = CPResponse.ERROR_RESPONSE.copy().setTextData(message);
        context.setData(CPNodeCommonKeys.RESPONSE, response);
    }

    /**
     * 写入仅包含单个字符串字段的简易成功响应体。
     */
    protected void writeSuccessField(DefaultContext context, ObjectMapper objectMapper,
                                     String fieldName, String fieldValue) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(fieldName, fieldValue);
        CPResponse response = CPResponse.SUCCESS_RESPONSE.copy().setData(node);
        context.setData(CPNodeCommonKeys.RESPONSE, response);
    }

    @Override
    public abstract void process(CPSession session, DefaultContext context, ObjectMapper objectMapper);
}
