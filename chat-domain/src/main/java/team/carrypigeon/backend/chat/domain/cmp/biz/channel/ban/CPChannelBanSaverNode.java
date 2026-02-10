package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;
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
    private final ApiWsEventPublisher wsEventPublisher;

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param session 当前请求会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取封禁实体并执行保存
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        Long targetUid = requireContext(context, CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID);
        Integer duration = context.get(CPNodeChannelBanKeys.CHANNEL_BAN_DURATION);
        Long untilTime = context.get(CPNodeChannelBanKeys.CHANNEL_BAN_UNTIL_TIME);
        String reason = context.get(CPNodeChannelBanKeys.CHANNEL_BAN_REASON);
        Long adminUid = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
        if ((untilTime == null || untilTime <= 0) && (duration == null || duration <= 0)) {
            validationFailed();
            return;
        }
        CPChannelBan ban = select(context,
                buildSelectKey("channel_ban", java.util.Map.of("cid", cid, "uid", targetUid)),
                () -> channelBanDAO.getByChannelIdAndUserId(targetUid, cid));
        if (ban == null) {
            ban = new CPChannelBan()
                    .setId(IdUtil.generateId())
                    .setCid(cid)
                    .setUid(targetUid);
        }
        long nowMillis = TimeUtil.currentTimeMillis();
        ban.setAid(adminUid)
                .setReason(reason)
                .setCreateTime(TimeUtil.currentLocalDateTime());
        if (untilTime != null && untilTime > 0) {
            long durationSec = Math.max(1L, (untilTime - nowMillis) / 1000L);
            ban.setUntilTime(untilTime);
            ban.setDuration(durationSec > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) durationSec);
        } else if (duration != null && duration > 0) {
            ban.setDuration(duration);
            ban.setUntilTime(nowMillis + duration.longValue() * 1000L);
        }
        if (!channelBanDAO.save(ban)) {
            log.error("CPChannelBanSaver save failed, cid={}, uid={}", cid, targetUid);
            fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "error saving channel ban"));
        }
        context.set(CPNodeChannelBanKeys.CHANNEL_BAN_INFO, ban);
        wsEventPublisher.publishChannelChangedToChannelMembers(cid, "bans");
        log.info("CPChannelBanSaver success, banId={}, cid={}, uid={}, untilTime={}, duration={}, reason={}",
                ban.getId(), ban.getCid(), ban.getUid(), ban.getUntilTime(), ban.getDuration(), ban.getReason());
    }
}
