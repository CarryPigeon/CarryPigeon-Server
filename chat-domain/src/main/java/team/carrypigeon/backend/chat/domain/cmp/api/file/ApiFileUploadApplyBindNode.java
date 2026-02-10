package team.carrypigeon.backend.chat.domain.cmp.api.file;

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
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeFileKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.FileUploadApplyRequest;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 文件上传申请绑定节点。
 * <p>
 * 解析 `POST /api/files/uploads` 请求，校验参数后构建 `CPFileInfo` 并写入上下文。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiFileUploadApplyBind")
public class ApiFileUploadApplyBindNode extends CPNodeComponent {

    private static final Pattern MIME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]*/[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]*$");

    private final ChannelMemberDao channelMemberDao;

    /**
     * 解析并绑定文件上传申请。
     */
    @Override
    protected void process(CPFlowContext context) {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof FileUploadApplyRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        if (req.filename() == null || req.filename().isBlank()) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "filename", "reason", "invalid", "message", "filename is required")
                    ))));
        }
        if (containsUnsafeChars(req.filename())) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "filename", "reason", "invalid", "message", "filename contains invalid chars")
                    ))));
        }
        long size = req.sizeBytes() == null ? 0L : req.sizeBytes();
        if (size <= 0) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "size_bytes", "reason", "invalid", "message", "size_bytes must be > 0")
                    ))));
        }
        if (req.mimeType() != null && !req.mimeType().isBlank() && !isValidMimeType(req.mimeType())) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "mime_type", "reason", "invalid", "message", "invalid mime_type")
                    ))));
        }

        CPFileAccessScopeEnum scope = CPFileAccessScopeEnum.parseOrDefault(req.scope());
        long scopeCid = 0L;
        if (scope == CPFileAccessScopeEnum.CHANNEL) {
            scopeCid = parseId(req.scopeCid(), "scope_cid");
            if (channelMemberDao.getMember(uid, scopeCid) == null) {
                throw new CPProblemException(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
            }
        }

        long fileId = IdUtil.generateId();
        String shareKey = generateShareKey();
        String objectName = "file_" + fileId;
        LocalDateTime now = TimeUtil.currentLocalDateTime();
        String sha256 = req.sha256();
        CPFileInfo info = new CPFileInfo()
                .setId(fileId)
                .setShareKey(shareKey)
                .setOwnerUid(uid)
                .setAccessScope(scope)
                .setScopeCid(scopeCid)
                .setScopeMid(0L)
                .setFilename(req.filename())
                .setSha256(sha256 == null || sha256.isBlank() ? null : sha256)
                .setSize(size)
                .setObjectName(objectName)
                .setContentType(req.mimeType() == null ? null : req.mimeType().trim())
                .setUploaded(false)
                .setUploadedTime(null)
                .setCreateTime(now);

        context.set(CPNodeFileKeys.FILE_INFO, info);
        context.set(CPNodeFileKeys.FILE_INFO_ID, Long.toString(info.getId()));
        context.set(CPNodeFileKeys.FILE_INFO_SHARE_KEY, info.getShareKey());
        context.set(CPNodeFileKeys.FILE_INFO_OWNER_UID, info.getOwnerUid());
        context.set(CPNodeFileKeys.FILE_INFO_FILENAME, info.getFilename());
        context.set(CPNodeFileKeys.FILE_INFO_SHA256, info.getSha256());
        context.set(CPNodeFileKeys.FILE_INFO_SIZE, info.getSize());
        context.set(CPNodeFileKeys.FILE_INFO_OBJECT_NAME, info.getObjectName());
        context.set(CPNodeFileKeys.FILE_INFO_CONTENT_TYPE, info.getContentType());
        context.set(CPNodeFileKeys.FILE_INFO_UPLOADED, info.isUploaded());
        context.set(CPNodeFileKeys.FILE_INFO_CREATE_TIME, info.getCreateTime() == null ? 0L : TimeUtil.localDateTimeToMillis(info.getCreateTime()));

        log.debug("ApiFileUploadApplyBind success, fileId={}, shareKey={}, uid={}", info.getId(), info.getShareKey(), uid);
    }

    /**
     * 生成文件分享键。
     */
    private String generateShareKey() {
        return "shr_" + UUID.randomUUID().toString().replace("-", "");
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
                            Map.of("field", field, "reason", "invalid", "message", "invalid " + field)
                    ))));
        }
    }

    /**
     * 校验 MIME 类型格式是否合法。
     */
    private boolean isValidMimeType(String mimeType) {
        String trimmed = mimeType.trim();
        if (trimmed.length() > 127) {
            return false;
        }
        return MIME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 检测文件名是否包含 CR/LF 注入字符。
     */
    private boolean containsUnsafeChars(String filename) {
        return filename.indexOf('\r') >= 0 || filename.indexOf('\n') >= 0;
    }
}
