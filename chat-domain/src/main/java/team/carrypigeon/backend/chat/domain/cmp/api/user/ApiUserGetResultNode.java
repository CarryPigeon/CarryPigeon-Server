package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 用户详情结果节点。
 * <p>
 * 将上下文中的用户实体映射为 `GET /api/users/{uid}` 响应体。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiUserGetResult")
public class ApiUserGetResultNode extends AbstractResultNode<ApiUserGetResultNode.UserProfile> {

    private final FileInfoDao fileInfoDao;

    /**
     * 构建用户详情响应。
     */
    @Override
    protected UserProfile build(CPFlowContext context) {
        CPUser user = requireContext(context, CPNodeUserKeys.USER_INFO);
        UserProfile resp = new UserProfile(
                Long.toString(user.getId()),
                user.getUsername(),
                avatarPath(user.getAvatar())
        );
        log.debug("ApiUserGetResult success, uid={}", user.getId());
        return resp;
    }

    /**
     * 根据头像文件 ID 生成下载路径。
     */
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

    /**
     * 用户详情响应体。
     */
    public record UserProfile(String uid, String nickname, String avatar) {
    }
}
