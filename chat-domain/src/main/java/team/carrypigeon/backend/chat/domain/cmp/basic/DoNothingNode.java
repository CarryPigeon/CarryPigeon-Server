package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 什么都不做的节点,用于规避THEN的空检查
 */
@LiteflowComponent("DoNothing")
public class DoNothingNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {}
}
