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
 * CPController的抽象类，用于对较为复杂的controller实现进行process拆分<br/>
 * 过于简单的业务处理可直接继承顶级接口
 * @author midreamsheep
 * */
public abstract class CPControllerAbstract<T> implements CPController{

    protected final ObjectMapper objectMapper;

    protected final Class<T> VOClazz;

    public CPControllerAbstract(ObjectMapper objectMapper, Class<T> clazz) {
        this.objectMapper = objectMapper;
        this.VOClazz = clazz;
    }

    @Override
    public CPResponse process(CPSession session, JsonNode data) {
        // 上下文数据，用于不同流程间的参数传递
        Map<String,Object> context = new HashMap<>();
        T vo;
        // 数据解析
        try {
            vo = objectMapper.treeToValue(data, VOClazz);
        } catch (JsonProcessingException e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 校验数据
        CPResponse checkResponse = check(session, vo,context);
        if (checkResponse!=null){
            return checkResponse;
        }
        // 处理业务逻辑
        CPResponse cpResponse = process0(session, vo,context);
        // 如果为成功处理则进入通知
        if (cpResponse.getCode() == SUCCESS_RESPONSE.getCode()){
            notify(session, vo,context);
        }
        return cpResponse;
    }

    /**
     * 校验数据 <br/>
     * 用于校验请求参数是否合法与进行用户权限校验<br/>
     * 配置文件的相关配置也在该方法中处理<br/>
     * 校验用户处理获取的数据库信息可存入上下文中用于下次使用<br/>
     *
     * @param session 用户会话
     * @param data    请求数据
     * @param context 上下文数据
     * @return null:数据合法 否则返回错误信息
     */
    protected abstract CPResponse check(CPSession session, T data, Map<String, Object> context);

    /**
     * 处理业务逻辑<br/>
     * 用于业务的具体处理，主要是数据的相关操作<br/>
     * @param session 用户会话
     * @param data 请求数据
     * @param context 上下文数据
     * @return 处理结果
     * */
    protected abstract CPResponse process0(CPSession session, T data, Map<String, Object> context);

    /**
     * 通知<br/>
     * 用于处理后的通知处理，用于对监听相关数据状态的会话进行通知操作<br/>
     * 比如发送文本消息或者通道信息发生变动<br/>
     * @param session 用户会话
     * @param vo 请求数据
     * @param context 上下文数据
     * */
    protected void notify(CPSession session, T vo, Map<String, Object> context){/*可以留空*/}


}
