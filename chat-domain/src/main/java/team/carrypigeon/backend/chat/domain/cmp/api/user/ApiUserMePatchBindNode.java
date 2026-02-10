package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UserMePatchRequest;

import java.util.List;
import java.util.Map;

/**
 * 当前用户资料更新绑定节点。
 * <p>
 * 解析 `PATCH /api/users/me` 请求并写入可更新字段。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiUserMePatchBind")
public class ApiUserMePatchBindNode extends CPNodeComponent {

    private final FileInfoDao fileInfoDao;

    /**
     * 解析并绑定当前用户资料更新参数。
     */
    @Override
    protected void process(CPFlowContext context) {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof UserMePatchRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }

        boolean hasAny = false;
        context.set(CPNodeUserKeys.USER_INFO_ID, uid);

        if (req.nickname() != null) {
            String nickname = req.nickname().trim();
            if (nickname.isEmpty()) {
                throw fieldValidationFailed("nickname");
            }
            context.set(CPNodeUserKeys.USER_INFO_USER_NAME, nickname);
            hasAny = true;
        }

        if (req.avatar() != null) {
            String avatar = req.avatar().trim();
            if (avatar.isEmpty()) {
                context.set(CPNodeUserKeys.USER_INFO_AVATAR, 0L);
            } else {
                CPFileInfo info = fileInfoDao.getByShareKey(avatar);
                if (info == null || !info.isUploaded()) {
                    throw fieldValidationFailed("avatar");
                }
                if (info.getOwnerUid() != uid) {
                    throw new CPProblemException(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
                }
                context.set(CPNodeUserKeys.USER_INFO_AVATAR, info.getId());
            }
            hasAny = true;
        }

        if (!hasAny) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }

        log.debug("ApiUserMePatchBind success, uid={}", uid);
    }

    /**
     * 生成字段校验失败异常。
     */
    private CPProblemException fieldValidationFailed(String field) {
        return new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                Map.of("field_errors", List.of(
                        Map.of("field", field, "reason", "invalid", "message", "invalid " + field)
                ))));
    }
}
