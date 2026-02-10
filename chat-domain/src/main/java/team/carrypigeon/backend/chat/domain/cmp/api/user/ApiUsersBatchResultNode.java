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
 * 用户批量查询结果节点。
 * <p>
 * 将批量命中的用户实体集合映射为 API 返回列表。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiUsersBatchResult")
public class ApiUsersBatchResultNode extends AbstractResultNode<ApiUsersBatchResultNode.UsersBatchResponse> {

    private final FileInfoDao fileInfoDao;

    /**
     * 构建用户批量查询响应。
     */
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
     * 批量用户响应体。
     */
    public record UsersBatchResponse(List<UserItem> items) {
    }

    /**
     * 单个用户响应项。
     */
    public record UserItem(String uid, String nickname, String avatar) {
    }
}
