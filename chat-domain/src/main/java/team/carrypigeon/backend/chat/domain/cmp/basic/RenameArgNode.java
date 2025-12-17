package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import java.util.Map;

/**
 * 重命名参数节点。<br/>
 * 通过 data 传入重命名脚本：<br/>
 * 在 LiteFlow EL 中使用：
 * <pre>
 *     RenameScript = {"from1":"to1","from2":"to2"};
 *     XxxNode, RenameArg.data(RenameScript), YyyNode
 * </pre>
 * 其中 key 为源 key（from），value 为目标 key（to），节点会执行：
 * {@code context.setData(to, context.getData(from))}。<br/>
 * 注：重命名参数只会新增新 key，不会删除原参数。<br/>
 */
@Slf4j
@LiteflowComponent("RenameArg")
public class RenameArgNode extends CPNodeComponent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        // 从节点 data 中读取重命名脚本：
        // - 推荐形式：字符串 JSON，如 '{"SessionId":"UserInfo_Id"}'
        // - 兼容旧形式：Map<String,String>
        Object scriptObj = this.getCmpData(Object.class);
        Map<?, ?> mapping;
        if (scriptObj instanceof String scriptStr) {
            try {
                mapping = OBJECT_MAPPER.readValue(scriptStr, new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                log.error("RenameArg args error: invalid JSON script in node {}, script={}",
                        getNodeId(), scriptStr, e);
                argsError(context);
                return;
            }
        } else if (scriptObj instanceof Map<?, ?> map) {
            mapping = map;
        } else {
            log.error("RenameArg args error: data is neither JSON string nor Map in node {}, actual={}",
                    getNodeId(), scriptObj == null ? null : scriptObj.getClass().getName());
            argsError(context);
            return;
        }

        for (Map.Entry<?, ?> entry : mapping.entrySet()) {
            Object fromObj = entry.getKey();
            Object toObj = entry.getValue();
            if (!(fromObj instanceof String) || !(toObj instanceof String)) {
                log.warn("RenameArg skip entry with non-string key/value: from={}, to={}, node={}",
                        fromObj, toObj, getNodeId());
                continue;
            }
            String from = (String) fromObj;
            String to = (String) toObj;
            Object value = context.getData(from);
            context.setData(to, value);
        }
    }
}
