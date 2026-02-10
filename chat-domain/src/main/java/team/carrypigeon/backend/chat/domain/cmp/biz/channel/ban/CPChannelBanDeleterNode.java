package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

/**
 * Delete a channel ban record.
 * Input: ChannelBanInfo(CPChannelBan).
 * Output: none.
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanDeleter")
public class CPChannelBanDeleterNode extends AbstractDeleteNode<CPChannelBan> {

    private final ChannelBanDAO channelBanDAO;
    private final ApiWsEventPublisher wsEventPublisher;

    /**
     * 返回当前实体在上下文中的键。
     *
     * @return 封禁实体在上下文中的键 { CHANNEL_BAN_INFO}
     */
    @Override
    protected CPKey<CPChannelBan> getContextKey() {
        return CPNodeChannelBanKeys.CHANNEL_BAN_INFO;
    }

    /**
     * 返回当前节点处理的实体类型。
     *
     * @return 组件处理的实体类型 { CPChannelBan}
     */
    @Override
    protected Class<CPChannelBan> getEntityClass() {
        return CPChannelBan.class;
    }

    /**
     * 执行删除操作并返回是否成功。
     *
     * @param ban 待删除的封禁实体
     * @return {@code true} 表示封禁记录删除成功
     */
    @Override
    protected boolean doDelete(CPChannelBan ban) {
        return channelBanDAO.delete(ban);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 删除失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "error deleting channel ban";
    }

    /**
     * 处理失败后的补偿逻辑。
     *
     * @param ban 删除失败的封禁实体（可能为 {@code null}）
     * @param context LiteFlow 上下文，用于中断链路并返回统一错误
     */
    @Override
    protected void onFailure(CPChannelBan ban, CPFlowContext context) {
        if (ban != null) {
            log.error("CPChannelBanDeleter delete failed, banId={}, cid={}, uid={}",
                    ban.getId(), ban.getCid(), ban.getUid());
        }
        fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "error deleting channel ban"));
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param ban 已删除成功的封禁实体
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void afterSuccess(CPChannelBan ban, CPFlowContext context) {
        log.info("CPChannelBanDeleter success, banId={}, cid={}, uid={}",
                ban.getId(), ban.getCid(), ban.getUid());
        wsEventPublisher.publishChannelChangedToChannelMembers(ban.getCid(), "bans");
    }
}
