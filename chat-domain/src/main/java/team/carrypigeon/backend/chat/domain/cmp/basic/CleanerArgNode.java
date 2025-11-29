package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;

/**
 * 清理参数节点。<br/>
 * 需要清理的参数必须以 ; 隔开，例如：name1;name2<br/>
 * 注：清理参数只会删除原参数。<br/>
 */
@LiteflowComponent("CleanerArg")
public class CleanerArgNode extends CPNodeComponent {

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String bindData = getBindData("key", String.class);
        if (bindData == null) {
            argsError(context);
            return;
        }
        String[] cleaners = bindData.split(";");
        for (String s : cleaners) {
            context.dataMap.remove(s);
        }
    }
}
