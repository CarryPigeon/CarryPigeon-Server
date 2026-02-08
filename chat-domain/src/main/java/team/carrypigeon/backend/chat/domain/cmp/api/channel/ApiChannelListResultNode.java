package team.carrypigeon.backend.chat.domain.cmp.api.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Result mapper for {@code GET /api/channels}.
 * <p>
 * Input (from {@code CPChannelCollector}):
 * {@link CPNodeChannelKeys#CHANNEL_INFO_LIST} = {@code Set<CPChannel>}
 * <p>
 * Output:
 * {@link ApiFlowKeys#RESPONSE} = {@link ChannelsResponse}
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiChannelListResult")
public class ApiChannelListResultNode extends AbstractResultNode<ApiChannelListResultNode.ChannelsResponse> {

    private final FileInfoDao fileInfoDao;

    @Override
    protected ChannelsResponse build(CPFlowContext context) {
        @SuppressWarnings("unchecked")
        Set<CPChannel> channels = (Set<CPChannel>) requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_LIST);

        List<Long> avatarIds = channels.stream()
                .filter(Objects::nonNull)
                .map(CPChannel::getAvatar)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        Map<Long, String> avatarShareKeys = fileInfoDao.listByIds(avatarIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CPFileInfo::getId, f -> f.getShareKey() == null ? "" : f.getShareKey(), (a, b) -> a));

        List<ChannelItem> items = channels.stream()
                .sorted(Comparator.comparingLong(CPChannel::getId))
                .map(c -> new ChannelItem(
                        Long.toString(c.getId()),
                        c.getName(),
                        c.getBrief() == null ? "" : c.getBrief(),
                        avatarPath(c.getAvatar(), avatarShareKeys),
                        Long.toString(c.getOwner())
                ))
                .toList();
        ChannelsResponse response = new ChannelsResponse(items);
        log.debug("ApiChannelListResult success, size={}", items.size());
        return response;
    }

    private String avatarPath(long avatarId, Map<Long, String> avatarShareKeys) {
        if (avatarId <= 0) {
            return "";
        }
        String shareKey = avatarShareKeys.get(avatarId);
        if (shareKey == null || shareKey.isBlank()) {
            return "";
        }
        return "api/files/download/" + shareKey;
    }

    public record ChannelsResponse(List<ChannelItem> channels) {
    }

    public record ChannelItem(String cid, String name, String brief, String avatar, String ownerUid) {
    }
}
