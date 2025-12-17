package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;

/**
 * 清理参数节点。<br/>
 * 需要清理的参数必须以 ; 隔开，例如：name1;name2<br/>
 * 注：清理参数只会删除原参数。<br/>
 */
@Slf4j
@LiteflowComponent("CleanerArg")
public class CleanerArgNode extends CPNodeComponent {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        String bindData = getBindData(CPNodeBindKeys.KEY, String.class);
        if (bindData == null) {
            log.error("CleanerArg args error: bind key '{}' is missing or null in node {}", CPNodeBindKeys.KEY, getNodeId());
            argsError(context);
            return;
        }
        String[] cleaners = bindData.split(";");
        for (String s : cleaners) {
            context.dataMap.remove(s);
        }
    }
}
