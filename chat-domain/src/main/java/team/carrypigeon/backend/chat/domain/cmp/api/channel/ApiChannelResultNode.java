package team.carrypigeon.backend.chat.domain.cmp.api.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * Result mapper for channel endpoints that return a single channel.
 * <p>
 * Input: {@link CPNodeChannelKeys#CHANNEL_INFO}
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiChannelResult")
public class ApiChannelResultNode extends AbstractResultNode<ApiChannelResultNode.ChannelResponse> {

    private final FileInfoDao fileInfoDao;

    @Override
    protected ChannelResponse build(CPFlowContext context) {
        CPChannel channel = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);
        long createTime = channel.getCreateTime() == null ? 0L : TimeUtil.localDateTimeToMillis(channel.getCreateTime());
        ChannelResponse resp = new ChannelResponse(
                Long.toString(channel.getId()),
                channel.getName(),
                channel.getBrief() == null ? "" : channel.getBrief(),
                avatarPath(channel.getAvatar()),
                Long.toString(channel.getOwner()),
                createTime
        );
        log.debug("ApiChannelResult success, cid={}", channel.getId());
        return resp;
    }

    private String avatarPath(long avatarId) {
        if (avatarId <= 0) {
            return "";
        }
        var info = fileInfoDao.getById(avatarId);
        if (info == null || info.getShareKey() == null || info.getShareKey().isBlank()) {
            return "";
        }
        return "api/files/download/" + info.getShareKey();
    }

    public record ChannelResponse(String cid, String name, String brief, String avatar, String ownerUid, long createTime) {
    }
}
