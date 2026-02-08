package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * List channel ban records.
 * Input: ChannelInfo_Id(Long).
 * Output: ChannelBanItems(List<CPChannelBan>).
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanLister")
public class CPChannelBanListerNode extends CPNodeComponent {

    private final ChannelBanDAO channelBanDAO;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        CPChannelBan[] bans = select(context,
                buildSelectKey("channel_ban", "cid", cid),
                () -> channelBanDAO.getByChannelId(cid));
        List<CPChannelBan> items = new ArrayList<>();
        if (bans != null) {
            for (CPChannelBan ban : bans) {
                if (ban.getUntilTime() <= 0 && ban.getCreateTime() != null && ban.getDuration() > 0) {
                    long until = TimeUtil.localDateTimeToMillis(ban.getCreateTime()) + ban.getDuration() * 1000L;
                    ban.setUntilTime(until);
                }
                if (ban.isValid()) {
                    items.add(ban);
                } else {
                    boolean deleted = channelBanDAO.delete(ban);
                    if (deleted) {
                        log.debug("CPChannelBanLister deleted expired ban, banId={}, cid={}, uid={}",
                                ban.getId(), ban.getCid(), ban.getUid());
                    } else {
                        log.warn("CPChannelBanLister failed to delete expired ban, banId={}, cid={}, uid={}",
                                ban.getId(), ban.getCid(), ban.getUid());
                    }
                }
            }
        }
        context.set(CPNodeChannelBanKeys.CHANNEL_BAN_ITEMS, items);
        log.debug("CPChannelBanLister success, cid={}, size={}", cid, items.size());
    }
}
