package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * Delete a channel ban record.
 * Input: ChannelBanInfo(CPChannelBan).
 * Output: none.
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanDeleter")
public class CPChannelBanDeleterNode extends CPNodeComponent {

    private final ChannelBanDAO channelBanDAO;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelBan ban = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_INFO);
        if (ban == null) {
            log.error("CPChannelBanDeleter args error: ChannelBanInfo is null");
            argsError(context);
            return;
        }
        if (!channelBanDAO.delete(ban)) {
            log.error("CPChannelBanDeleter delete failed, banId={}, cid={}, uid={}",
                    ban.getId(), ban.getCid(), ban.getUid());
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("error deleting channel ban"));
            throw new CPReturnException();
        }
        log.info("CPChannelBanDeleter success, banId={}, cid={}, uid={}",
                ban.getId(), ban.getCid(), ban.getUid());
    }
}
