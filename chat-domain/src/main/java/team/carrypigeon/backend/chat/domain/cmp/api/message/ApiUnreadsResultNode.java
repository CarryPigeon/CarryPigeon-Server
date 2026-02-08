package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 未读计数结果节点（HTTP：{@code GET /api/unreads}）。
 *
 * <p>推荐链路组合：{@code ApiLoginGuard -> CPChannelCollector -> ApiUnreadsResult}
 *
 * <p>输入：
 * <ul>
 *   <li>{@link CPFlowKeys#SESSION_UID}：当前 uid</li>
 *   <li>{@link CPNodeChannelKeys#CHANNEL_INFO_LIST}：可见频道集合（由 {@code CPChannelCollector} 提供）</li>
 * </ul>
 *
 * <p>输出：{@link CPFlowKeys#RESPONSE} = {@link UnreadsResponse}
 *
 * <p>实现说明：LiteFlow DSL 不擅长表达“对集合逐项循环”的场景；因此本节点内部自行循环并直接查询 DAO，
 * 避免“节点调用节点”的隐式依赖（会降低可审计性，也更难做统一的权限/异常约束）。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiUnreadsResult")
public class ApiUnreadsResultNode extends AbstractResultNode<ApiUnreadsResultNode.UnreadsResponse> {

    /** 读状态 DAO（查询 last_read_mid/last_read_time）。 */
    private final ChannelReadStateDao channelReadStateDao;
    /** 消息 DAO（按 last_read_mid 统计未读数）。 */
    private final ChannelMessageDao channelMessageDao;

    /**
     * 构造未读计数响应体。
     *
     * <p>依赖上下文：
     * <ul>
     *   <li>{@link CPFlowKeys#SESSION_UID}</li>
     *   <li>{@link CPNodeChannelKeys#CHANNEL_INFO_LIST}</li>
     * </ul>
     */
    @Override
    protected UnreadsResponse build(CPFlowContext context) throws Exception {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);

        Set<CPChannel> channels = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_LIST);

        List<CPChannel> sortedChannels = channels.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(CPChannel::getId))
                .toList();

        List<UnreadItem> items = new ArrayList<>();
        for (CPChannel c : sortedChannels) {
            long cid = c.getId();

            CPChannelReadState state = select(context,
                    buildSelectKey("channel_read_state", java.util.Map.of("cid", cid, "uid", uid)),
                    () -> channelReadStateDao.getByUidAndCid(uid, cid));
            long lastReadTime = state == null ? 0L : Optional.ofNullable(state.getLastReadTime()).orElse(0L);
            long lastReadMid = state == null ? 0L : Optional.ofNullable(state.getLastReadMid()).orElse(0L);

            Integer countObj = select(context,
                    buildSelectKey("message_unread_count", java.util.Map.of("cid", cid, "startMid", lastReadMid)),
                    () -> channelMessageDao.countAfter(cid, lastReadMid));
            int count = countObj == null ? 0 : Math.max(0, countObj);

            items.add(new UnreadItem(Long.toString(cid), count, lastReadTime));
        }

        UnreadsResponse response = new UnreadsResponse(items);
        log.debug("未读计数结果映射：完成：uid={}, channels={}", uid, items.size());
        return response;
    }

    /**
     * 未读计数响应体。
     */
    public record UnreadsResponse(List<UnreadItem> items) {
    }

    /**
     * 单频道未读信息。
     */
    public record UnreadItem(String cid, int unreadCount, long lastReadTime) {
    }
}
