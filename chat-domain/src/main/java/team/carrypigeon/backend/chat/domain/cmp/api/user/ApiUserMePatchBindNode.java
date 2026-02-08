package team.carrypigeon.backend.chat.domain.cmp.api.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.UserMePatchRequest;

import java.util.List;
import java.util.Map;

/**
 * Bind {@code PATCH /api/users/me} request body into {@link CPNodeUserKeys}.
 * <p>
 * Output keys:
 * <ul>
 *   <li>{@link CPNodeUserKeys#USER_INFO_ID}</li>
 *   <li>{@link CPNodeUserKeys#USER_INFO_USER_NAME} (optional)</li>
 *   <li>{@link CPNodeUserKeys#USER_INFO_AVATAR} (optional)</li>
 * </ul>
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiUserMePatchBind")
public class ApiUserMePatchBindNode extends CPNodeComponent {

    private final FileInfoDao fileInfoDao;

    @Override
    protected void process(CPFlowContext context) {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof UserMePatchRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
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
                    throw new CPProblemException(CPProblem.of(403, "forbidden", "forbidden"));
                }
                context.set(CPNodeUserKeys.USER_INFO_AVATAR, info.getId());
            }
            hasAny = true;
        }

        if (!hasAny) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }

        log.debug("ApiUserMePatchBind success, uid={}", uid);
    }

    private CPProblemException fieldValidationFailed(String field) {
        return new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                Map.of("field_errors", List.of(
                        Map.of("field", field, "reason", "invalid", "message", "invalid " + field)
                ))));
    }
}
