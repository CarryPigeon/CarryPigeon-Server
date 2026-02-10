package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * Select a valid channel ban record by channel id and target uid.
 * Input: ChannelInfo_Id(Long), ChannelBan_TargetUid(Long).
 * Output: ChannelBanInfo(CPChannelBan).
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanSelector")
public class CPChannelBanSelectorNode extends AbstractSelectorNode<CPChannelBan> {

    private final ChannelBanDAO channelBanDAO;

    /**
     * 读取当前节点的执行模式。
     *
     * @param context LiteFlow 上下文（此节点固定使用 default 模式）
     * @return 固定模式字符串 { default}
     */
    @Override
    protected String readMode(CPFlowContext context) {
        return "default";
    }

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（当前固定为 { default}）
     * @param context LiteFlow 上下文（此节点固定使用 default 模式）
     * @return 命中的有效封禁记录；未命中或已过期返回 { null}
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected CPChannelBan doSelect(String mode, CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        Long targetUid = requireContext(context, CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID);
        CPChannelBan ban = select(context,
                buildSelectKey("channel_ban", java.util.Map.of("cid", cid, "uid", targetUid)),
                () -> channelBanDAO.getByChannelIdAndUserId(targetUid, cid));
        if (ban == null || !ban.isValid()) {
            return null;
        }
        return ban;
    }

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 封禁实体写入键 { CHANNEL_BAN_INFO}
     */
    @Override
    protected CPKey<CPChannelBan> getResultKey() {
        return CPNodeChannelBanKeys.CHANNEL_BAN_INFO;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式
     * @param context LiteFlow 上下文（此节点固定使用 default 模式）
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        log.info("CPChannelBanSelector: ban not found or expired");
        fail(CPProblem.of(CPProblemReason.NOT_FOUND, "this user is not banned"));
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param mode 查询模式
     * @param entity 查询成功的封禁实体
     * @param context LiteFlow 上下文（此节点固定使用 default 模式）
     */
    @Override
    protected void afterSuccess(String mode, CPChannelBan entity, CPFlowContext context) {
        log.debug("CPChannelBanSelector success, banId={}, cid={}, uid={}",
                entity.getId(), entity.getCid(), entity.getUid());
    }
}
