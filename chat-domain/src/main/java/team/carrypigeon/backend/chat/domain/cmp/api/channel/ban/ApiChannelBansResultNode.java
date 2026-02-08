package team.carrypigeon.backend.chat.domain.cmp.api.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Result mapper for {@code GET /api/channels/{cid}/bans}.
 */
@Slf4j
@LiteflowComponent("ApiChannelBansResult")
public class ApiChannelBansResultNode extends AbstractResultNode<ApiChannelBansResultNode.BansResponse> {

    @Override
    protected BansResponse build(CPFlowContext context) {
        @SuppressWarnings("unchecked")
        List<CPChannelBan> bans = (List<CPChannelBan>) requireContext(context, CPNodeChannelBanKeys.CHANNEL_BAN_ITEMS);
        List<BanItem> items = bans.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(CPChannelBan::getId))
                .map(b -> new BanItem(
                        Long.toString(b.getCid()),
                        Long.toString(b.getUid()),
                        b.getUntilTime(),
                        b.getReason() == null ? "" : b.getReason(),
                        b.getCreateTime() == null ? 0L : TimeUtil.localDateTimeToMillis(b.getCreateTime())
                ))
                .toList();
        BansResponse resp = new BansResponse(items);
        log.debug("ApiChannelBansResult success, size={}", items.size());
        return resp;
    }

    public record BansResponse(List<BanItem> items) {
    }

    public record BanItem(String cid, String uid, long until, String reason, long createTime) {
    }
}

