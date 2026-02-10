package team.carrypigeon.backend.chat.domain.cmp.biz.file;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileAccessScopeEnum;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * Promote current user's avatar file to {@link CPFileAccessScopeEnum#AUTH}.
 * <p>
 * Reason:
 * user/channel avatars are downloaded by other logged-in users (message list, member list, etc).
 * Without this step, avatar files created with default scope {@code OWNER} would become invisible to others.
 * <p>
 * Input:
 * <ul>
 *   <li>{@link CPFlowKeys#SESSION_UID}</li>
 *   <li>{@link CPNodeUserKeys#USER_INFO_AVATAR} (optional; only runs when present and &gt; 0)</li>
 * </ul>
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPUserAvatarFileScopeUpdater")
public class CPUserAvatarFileScopeUpdaterNode extends CPNodeComponent {

    private final FileInfoDao fileInfoDao;

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，读取头像文件并更新访问范围
     */
    @Override
    protected void process(CPFlowContext context) {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Long avatarId = context.get(CPNodeUserKeys.USER_INFO_AVATAR);
        if (avatarId == null || avatarId <= 0) {
            return;
        }

        CPFileInfo info = fileInfoDao.getById(avatarId);
        if (info == null || !info.isUploaded()) {
            fail(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
            return;
        }
        if (info.getOwnerUid() != uid) {
            fail(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
            return;
        }

        if (info.getAccessScope() != CPFileAccessScopeEnum.AUTH) {
            info.setAccessScope(CPFileAccessScopeEnum.AUTH).setScopeCid(0L).setScopeMid(0L);
            if (!fileInfoDao.save(info)) {
                fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "failed to save file info"));
            }
        }

        log.debug("CPUserAvatarFileScopeUpdater success, uid={}, fileId={}", uid, avatarId);
    }
}

