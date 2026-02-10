package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;

/**
 * 当前登录用户结果节点。
 * <p>
 * 输出 `GET /api/users/me` 的用户资料响应。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiUserMeResult")
public class ApiUserMeResultNode extends AbstractResultNode<ApiUserMeResultNode.MeResponse> {

    private final UserDao userDao;
    private final FileInfoDao fileInfoDao;

    /**
     * 构建当前用户响应。
     */
    @Override
    protected MeResponse build(CPFlowContext context) {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        CPUser user = userDao.getById(uid);
        if (user == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.UNAUTHORIZED, "user not found"));
        }
        MeResponse response = new MeResponse(
                Long.toString(user.getId()),
                user.getEmail(),
                user.getUsername(),
                avatarPath(user.getAvatar())
        );
        log.debug("ApiUserMeResult success, uid={}", uid);
        return response;
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
     * 当前用户响应体。
     */
    public record MeResponse(String uid, String email, String nickname, String avatar) {
    }
}
