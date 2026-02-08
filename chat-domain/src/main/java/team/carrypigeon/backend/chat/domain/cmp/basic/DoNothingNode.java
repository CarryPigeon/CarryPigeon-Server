package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 空节点（占位用）。
 * <p>
 * 用于：
 * <ul>
 *     <li>规避某些 LiteFlow 规则在 THEN 为空时的校验/解析问题</li>
 *     <li>作为链路的占位节点，方便后续插入真实节点</li>
 * </ul>
 *
 * <p>入参：无；出参：无。</p>
 */
@LiteflowComponent("DoNothing")
public class DoNothingNode extends CPNodeComponent {
    @Override
    protected void process(CPFlowContext context) {
        // intentionally no-op
    }
}
