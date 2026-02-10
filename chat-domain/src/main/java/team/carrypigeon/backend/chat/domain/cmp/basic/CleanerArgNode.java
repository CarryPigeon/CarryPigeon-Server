package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeBindKeys;

/**
 * 清理上下文参数节点（从 {@link CPFlowContext} 移除 key）。
 * <p>
 * bind 参数：
 * <ul>
 *     <li>{@code key}：要删除的 key 列表，使用 {@code ;} 分隔（例如 {@code "a;b;c"}）</li>
 * </ul>
 *
 * <p>入参：无（仅依赖 bind 参数）。</p>
 * <p>出参：无（副作用：从上下文移除指定 key）。</p>
 */
@Slf4j
@LiteflowComponent("CleanerArg")
public class CleanerArgNode extends CPNodeComponent {

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param context LiteFlow 上下文，按 bind 指定键删除数据
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        String bindData = requireBind(CPNodeBindKeys.KEY, String.class);
        String[] cleaners = bindData.split(";");
        for (String s : cleaners) {
            context.dataMap.remove(s);
        }
    }
}
