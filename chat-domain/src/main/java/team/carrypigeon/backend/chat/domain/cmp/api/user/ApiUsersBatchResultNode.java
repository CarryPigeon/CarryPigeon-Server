package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Batch user profiles response for {@code GET /api/users?ids=...}.
 * <p>
 * Input: {@link CPNodeUserKeys#USER_INFO_LIST} from {@code CPUserGroupSelector}
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiUsersBatchResult")
public class ApiUsersBatchResultNode extends AbstractResultNode<ApiUsersBatchResultNode.UsersBatchResponse> {

    private final FileInfoDao fileInfoDao;

    @Override
    protected UsersBatchResponse build(CPFlowContext context) {
        @SuppressWarnings("unchecked")
        List<CPUser> users = (List<CPUser>) requireContext(context, CPNodeUserKeys.USER_INFO_LIST);

        List<Long> avatarIds = users.stream()
                .filter(Objects::nonNull)
                .map(CPUser::getAvatar)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        Map<Long, String> avatarShareKeys = fileInfoDao.listByIds(avatarIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CPFileInfo::getId, f -> f.getShareKey() == null ? "" : f.getShareKey(), (a, b) -> a));

        List<UserItem> items = users.stream()
                .filter(Objects::nonNull)
                .map(u -> new UserItem(Long.toString(u.getId()), u.getUsername(), avatarPath(u.getAvatar(), avatarShareKeys)))
                .toList();
        UsersBatchResponse resp = new UsersBatchResponse(items);
        log.debug("ApiUsersBatchResult success, size={}", items.size());
        return resp;
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

    public record UsersBatchResponse(List<UserItem> items) {
    }

    public record UserItem(String uid, String nickname, String avatar) {
    }
}
