package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list.CPChannelListBanResultItem;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * List channel ban records and convert them to result items.
 * Input: ChannelInfo_Id(Long).
 * Output: ChannelBanItems(List<CPChannelListBanResultItem>).
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanLister")
public class CPChannelBanListerNode extends CPNodeComponent {

    private final ChannelBanDAO channelBanDAO;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long cid = context.getData(CPNodeChannelKeys.CHANNEL_INFO_ID);
        if (cid == null) {
            log.error("CPChannelBanLister args error: ChannelInfo_Id is null");
            argsError(context);
            return;
        }
        CPChannelBan[] bans = channelBanDAO.getByChannelId(cid);
        List<CPChannelListBanResultItem> items = new ArrayList<>();
        if (bans != null) {
            for (CPChannelBan ban : bans) {
                if (ban.isValid()) {
                    CPChannelListBanResultItem item = new CPChannelListBanResultItem()
                            .setUid(ban.getUid())
                            .setAid(ban.getAid())
                            .setBanTime(TimeUtil.LocalDateTimeToMillis(ban.getCreateTime()))
                            .setDuration(ban.getDuration());
                    items.add(item);
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
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_ITEMS, items);
        log.debug("CPChannelBanLister success, cid={}, size={}", cid, items.size());
    }
}
