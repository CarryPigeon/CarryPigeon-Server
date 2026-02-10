package team.carrypigeon.backend.chat.domain.cmp.api.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileAccessScopeEnum;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelPatchInternalRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelPatchRequest;

import java.util.List;
import java.util.Map;

/**
 * 频道资料更新请求绑定节点。
 * <p>
 * 解析 `PATCH /api/channels/{cid}` 的可选字段，并把待更新字段写入上下文。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiChannelPatchBind")
public class ApiChannelPatchBindNode extends CPNodeComponent {

    private final FileInfoDao fileInfoDao;

    /**
     * 解析并绑定频道更新请求。
     */
    @Override
    protected void process(CPFlowContext context) {
        Long operatorUid = requireContext(context, CPFlowKeys.SESSION_UID);
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ChannelPatchInternalRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        ChannelPatchRequest body = req.body();
        if (body == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }

        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);

        if (body.name() != null) {
            if (body.name().isBlank()) {
                throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                        Map.of("field_errors", List.of(
                                Map.of("field", "name", "reason", "invalid", "message", "name cannot be blank")
                        ))));
            }
            context.set(CPNodeChannelKeys.CHANNEL_INFO_NAME, body.name());
        }
        if (body.brief() != null) {
            context.set(CPNodeChannelKeys.CHANNEL_INFO_BRIEF, body.brief());
        }
        if (body.avatar() != null) {
            long avatarId = resolveAvatarId(body.avatar(), operatorUid);
            context.set(CPNodeChannelKeys.CHANNEL_INFO_AVATAR, avatarId);
        }

        log.debug("ApiChannelPatchBind success, cid={}", cid);
    }

    /**
     * 解析字符串 ID。
     */
    private long parseId(String str, String field) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }

    /**
     * 解析头像标识并校验头像文件归属与访问范围。
     */
    private long resolveAvatarId(String avatar, long operatorUid) {
        if (avatar == null || avatar.isBlank()) {
            return 0L;
        }
        String raw = avatar.trim();
        if (raw.startsWith("shr_")) {
            raw = raw.substring("shr_".length());
        }
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
        }
        CPFileInfo info = fileInfoDao.getByShareKey(avatar);
        if (info == null || !info.isUploaded()) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "avatar", "reason", "invalid", "message", "invalid avatar")
                    ))));
        }
        if (info.getOwnerUid() != operatorUid) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
        }
        if (info.getAccessScope() != CPFileAccessScopeEnum.AUTH) {
            info.setAccessScope(CPFileAccessScopeEnum.AUTH).setScopeCid(0L).setScopeMid(0L);
            if (!fileInfoDao.save(info)) {
                throw new CPProblemException(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "failed to save file info"));
            }
        }
        return info.getId();
    }
}
