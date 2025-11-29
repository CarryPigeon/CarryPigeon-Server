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
 * Select a valid channel ban record by channel id and target uid.
 * Input: ChannelInfo_Id(Long), ChannelBan_TargetUid(Long).
 * Output: ChannelBanInfo(CPChannelBan).
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanSelector")
public class CPChannelBanSelectorNode extends CPNodeComponent {

    private final ChannelBanDAO channelBanDAO;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        Long targetUid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_TARGET_UID);
        if (cid == null || targetUid == null) {
            log.error("CPChannelBanSelector args error, cid={}, targetUid={}", cid, targetUid);
            argsError(context);
            return;
        }
        CPChannelBan ban = channelBanDAO.getByChannelIdAndUserId(targetUid, cid);
        if (ban == null || !ban.isValid()) {
            log.info("CPChannelBanSelector: ban not found or expired, cid={}, uid={}", cid, targetUid);
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("this user is not banned"));
            throw new CPReturnException();
        }
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_INFO, ban);
        log.debug("CPChannelBanSelector success, banId={}, cid={}, uid={}",
                ban.getId(), ban.getCid(), ban.getUid());
    }
}
