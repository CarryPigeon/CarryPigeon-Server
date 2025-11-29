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
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * Save or update channel ban record.
 * Input: ChannelInfo_Id(Long), ChannelBan_TargetUid(Long),
 *        ChannelBan_Duration(Integer, seconds), UserInfo_Id(Long admin id).
 * Output: ChannelBanInfo(CPChannelBan).
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanSaver")
public class CPChannelBanSaverNode extends CPNodeComponent {

    private final ChannelBanDAO channelBanDAO;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        Long targetUid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_TARGET_UID);
        Integer duration = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_DURATION);
        Long adminUid = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_ID);
        if (cid == null || targetUid == null || duration == null || adminUid == null) {
            log.error("CPChannelBanSaver args error, cid={}, targetUid={}, duration={}, adminUid={}",
                    cid, targetUid, duration, adminUid);
            argsError(context);
            return;
        }
        CPChannelBan ban = channelBanDAO.getByChannelIdAndUserId(targetUid, cid);
        if (ban == null) {
            ban = new CPChannelBan()
                    .setId(IdUtil.generateId())
                    .setCid(cid)
                    .setUid(targetUid);
        }
        ban.setAid(adminUid)
                .setDuration(duration)
                .setCreateTime(TimeUtil.getCurrentLocalTime());
        if (!channelBanDAO.save(ban)) {
            log.error("CPChannelBanSaver save failed, cid={}, uid={}", cid, targetUid);
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("error saving channel ban"));
            throw new CPReturnException();
        }
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_INFO, ban);
        log.info("CPChannelBanSaver success, banId={}, cid={}, uid={}, duration={}",
                ban.getId(), ban.getCid(), ban.getUid(), ban.getDuration());
    }
}
