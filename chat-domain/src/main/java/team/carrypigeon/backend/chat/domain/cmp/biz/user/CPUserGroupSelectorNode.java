package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Batch load users by ids.
 * <p>
 * Input:
 * {@link CPNodeUserKeys#USER_INFO_ID_LIST} = {@code List<Long>}
 * <p>
 * Output:
 * {@link CPNodeUserKeys#USER_INFO_LIST} = {@code List<CPUser>} (same order as input ids; missing ids are skipped)
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPUserGroupSelector")
public class CPUserGroupSelectorNode extends CPNodeComponent {

    private final UserDao userDao;

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，读取用户 ID 并加载所属频道集合
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) requireContext(context, CPNodeUserKeys.USER_INFO_ID_LIST);
        if (ids.isEmpty()) {
            context.set(CPNodeUserKeys.USER_INFO_LIST, List.of());
            return;
        }
        List<CPUser> users = userDao.listByIds(ids);
        Map<Long, CPUser> byId = new HashMap<>();
        for (CPUser u : users) {
            if (u != null) {
                byId.put(u.getId(), u);
            }
        }
        List<CPUser> ordered = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            CPUser u = byId.get(id);
            if (u != null) {
                ordered.add(u);
            }
        }
        context.set(CPNodeUserKeys.USER_INFO_LIST, ordered);
        log.debug("CPUserGroupSelector success, requested={}, returned={}",
                ids.stream().filter(Objects::nonNull).distinct().count(), ordered.size());
    }
}

