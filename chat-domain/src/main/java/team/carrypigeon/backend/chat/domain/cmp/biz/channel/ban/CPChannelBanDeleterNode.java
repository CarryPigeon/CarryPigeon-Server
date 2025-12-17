package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;

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

    @Override
    protected String getContextKey() {
        return CPNodeChannelBanKeys.CHANNEL_BAN_INFO;
    }

    @Override
    protected Class<CPChannelBan> getEntityClass() {
        return CPChannelBan.class;
    }

    @Override
    protected boolean doDelete(CPChannelBan ban) {
        return channelBanDAO.delete(ban);
    }

    @Override
    protected String getErrorMessage() {
        return "error deleting channel ban";
    }

    @Override
    protected void onFailure(CPChannelBan ban, CPFlowContext context) throws CPReturnException {
        if (ban != null) {
            log.error("CPChannelBanDeleter delete failed, banId={}, cid={}, uid={}",
                    ban.getId(), ban.getCid(), ban.getUid());
        }
        businessError(context, "error deleting channel ban");
    }

    @Override
    protected void afterSuccess(CPChannelBan ban, CPFlowContext context) {
        log.info("CPChannelBanDeleter success, banId={}, cid={}, uid={}",
                ban.getId(), ban.getCid(), ban.getUid());
    }
}
