package team.carrypigeon.backend.chat.domain.cmp.api.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 频道成员列表结果节点。
 * <p>
 * 汇总成员关系、用户资料与头像信息，输出成员列表响应体。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiChannelMembersResult")
public class ApiChannelMembersResultNode extends AbstractResultNode<ApiChannelMembersResultNode.MembersResponse> {

    private final UserDao userDao;
    private final FileInfoDao fileInfoDao;

    /**
     * 构建频道成员列表响应。
     */
    @Override
    protected MembersResponse build(CPFlowContext context) {
        CPChannel channel = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);

        @SuppressWarnings("unchecked")
        Set<CPChannelMember> members = (Set<CPChannelMember>) requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_LIST);

        List<Long> uids = members.stream().filter(Objects::nonNull).map(CPChannelMember::getUid).distinct().toList();
        List<CPUser> users = uids.isEmpty() ? List.of() : userDao.listByIds(uids);
        Map<Long, CPUser> byUid = new HashMap<>();
        for (CPUser u : users) {
            if (u != null) {
                byUid.put(u.getId(), u);
            }
        }

        List<Long> avatarIds = users.stream()
                .filter(Objects::nonNull)
                .map(CPUser::getAvatar)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        Map<Long, String> avatarShareKeys = fileInfoDao.listByIds(avatarIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CPFileInfo::getId, f -> f.getShareKey() == null ? "" : f.getShareKey(), (a, b) -> a));

        List<MemberItem> items = members.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(CPChannelMember::getUid))
                .map(m -> {
                    CPUser u = byUid.get(m.getUid());
                    String nickname = (m.getName() != null && !m.getName().isBlank())
                            ? m.getName()
                            : (u == null ? "" : u.getUsername());
                    String avatar = u == null ? "" : avatarPath(u.getAvatar(), avatarShareKeys);
                    String role = roleOf(channel, m);
                    long joinTime = m.getJoinTime() == null ? 0L : TimeUtil.localDateTimeToMillis(m.getJoinTime());
                    return new MemberItem(Long.toString(m.getUid()), role, nickname, avatar, joinTime);
                })
                .toList();

        MembersResponse resp = new MembersResponse(items);
        log.debug("ApiChannelMembersResult success, cid={}, size={}", channel.getId(), items.size());
        return resp;
    }

    /**
     * 计算成员角色（owner/admin/member）。
     */
    private String roleOf(CPChannel channel, CPChannelMember member) {
        if (member.getUid() == channel.getOwner()) {
            return "owner";
        }
        if (member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            return "admin";
        }
        return "member";
    }

    /**
     * 根据头像文件 ID 生成下载路径。
     */
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

    /**
     * 成员列表响应体。
     */
    public record MembersResponse(List<MemberItem> items) {
    }

    /**
     * 单个成员响应项。
     */
    public record MemberItem(String uid, String role, String nickname, String avatar, long joinTime) {
    }
}
