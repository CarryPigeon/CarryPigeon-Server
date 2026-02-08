package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.bo.domain.file.CPFileAccessScopeEnum;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.AccessTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.ApiAuth;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.FileUploadApplyRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;
import team.carrypigeon.backend.chat.domain.service.file.FileService;
import team.carrypigeon.backend.chat.domain.service.file.FileTokenService;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * File upload/download endpoints under {@code /api/files}.
 */
@RestController
public class ApiFileController {

    private static final String CHAIN_FILES_UPLOADS_CREATE = "api_files_uploads_create";

    private final ApiFlowRunner flowRunner;
    private final FileInfoDao fileInfoDao;
    private final ChannelMemberDao channelMemberDao;
    private final FileTokenService fileTokenService;
    private final FileService fileService;
    private final AccessTokenService accessTokenService;

    public ApiFileController(ApiFlowRunner flowRunner,
                             FileInfoDao fileInfoDao,
                             ChannelMemberDao channelMemberDao,
                             FileTokenService fileTokenService,
                             FileService fileService,
                             AccessTokenService accessTokenService) {
        this.flowRunner = flowRunner;
        this.fileInfoDao = fileInfoDao;
        this.channelMemberDao = channelMemberDao;
        this.fileTokenService = fileTokenService;
        this.fileService = fileService;
        this.accessTokenService = accessTokenService;
    }

    /**
     * Apply an upload slot (metadata + upload token).
     * <p>
     * Route: {@code POST /api/files/uploads}
     * <p>
     * Chain: {@code api_files_uploads_create}
     */
    @PostMapping("/api/files/uploads")
    public Object applyUpload(@Valid @RequestBody FileUploadApplyRequest body, HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, body);
        ctx.set(CPFlowKeys.AUTH_UID, ApiAuth.requireUid(request));
        flowRunner.executeOrThrow(CHAIN_FILES_UPLOADS_CREATE, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Upload binary for a previously created file id.
     * <p>
     * Route: {@code PUT /api/files/upload/{file_id}}
     * <p>
     * Authorization:
     * <ul>
     *   <li>Bearer access token (same as other /api endpoints)</li>
     *   <li>One-time header {@code x-cp-upload-token}</li>
     * </ul>
     */
    @PutMapping("/api/files/upload/{file_id}")
    public ResponseEntity<Void> upload(@PathVariable("file_id") String fileId,
                                       HttpServletRequest request) throws Exception {
        long uid = ApiAuth.requireUid(request);

        String token = request.getHeader("x-cp-upload-token");
        FileTokenService.FileToken ft = fileTokenService.consume(token);
        if (ft == null || !"UPLOAD".equalsIgnoreCase(ft.getOp()) || ft.getUid() != uid || ft.getFileId() == null || !ft.getFileId().equals(fileId)) {
            throw new CPProblemException(CPProblem.of(401, "unauthorized", "invalid upload token"));
        }

        long id = parseLongId(fileId);
        CPFileInfo info = fileInfoDao.getById(id);
        if (info == null) {
            throw new CPProblemException(CPProblem.of(404, "not_found", "file not found"));
        }
        if (info.getOwnerUid() != uid) {
            throw new CPProblemException(CPProblem.of(403, "forbidden", "forbidden"));
        }

        long expectedSize = info.getSize();
        long contentLength = request.getContentLengthLong();
        if (contentLength > 0 && contentLength != expectedSize) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }

        try (InputStream in = request.getInputStream()) {
            fileService.uploadIfNotExists(info.getObjectName(), in, expectedSize, info.getContentType());
        }

        info.setUploaded(true).setUploadedTime(TimeUtil.currentLocalDateTime());
        if (!fileInfoDao.save(info)) {
            throw new CPProblemException(CPProblem.of(500, "internal_error", "failed to save file info"));
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Download file binary by share key.
     * <p>
     * Route: {@code GET /api/files/download/{share_key}}
     * <p>
     * Access policy (P1):
     * <ul>
     *   <li>{@code share_key="server_avatar"} is public and does not require login.</li>
     *   <li>Other files are controlled by {@code file_info.access_scope} (P2):</li>
     *   <li>{@code PUBLIC}: no login required</li>
     *   <li>{@code AUTH}: any logged-in user</li>
     *   <li>{@code OWNER}: only uploader</li>
     *   <li>{@code CHANNEL}: channel members only</li>
     * </ul>
     * <p>
     * Note: message-level ACL (e.g. "file bound to a specific message id") is reserved for P3+ via {@code scope_mid}.
     */
    @GetMapping("/api/files/download/{share_key}")
    public ResponseEntity<InputStreamResource> download(@PathVariable("share_key") String shareKey,
                                                        HttpServletRequest request) throws Exception {
        if (shareKey == null || shareKey.isBlank()) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        if ("server_avatar".equals(shareKey)) {
            return streamObject("server_avatar", null, null);
        }

        CPFileInfo info = resolveFileInfo(shareKey);
        if (info == null) {
            throw new CPProblemException(CPProblem.of(404, "not_found", "file not found"));
        }
        if (!info.isUploaded()) {
            throw new CPProblemException(CPProblem.of(404, "not_found", "file not found"));
        }

        CPFileAccessScopeEnum scope = info.getAccessScope() == null ? CPFileAccessScopeEnum.OWNER : info.getAccessScope();
        if (scope != CPFileAccessScopeEnum.PUBLIC) {
            Long uid = verifyBearerIfPresent(request);
            if (uid == null) {
                throw new CPProblemException(CPProblem.of(401, "unauthorized", "missing or invalid access token"));
            }
            if (scope == CPFileAccessScopeEnum.OWNER) {
                if (info.getOwnerUid() != uid) {
                    throw new CPProblemException(CPProblem.of(403, "forbidden", "forbidden"));
                }
            } else if (scope == CPFileAccessScopeEnum.CHANNEL) {
                long cid = info.getScopeCid();
                if (cid <= 0) {
                    throw new CPProblemException(CPProblem.of(500, "internal_error", "invalid file scope"));
                }
                if (channelMemberDao.getMember(uid, cid) == null) {
                    throw new CPProblemException(CPProblem.of(403, "forbidden", "forbidden"));
                }
            } else if (scope == CPFileAccessScopeEnum.AUTH) {
                // authenticated users can download
            } else {
                throw new CPProblemException(CPProblem.of(403, "forbidden", "forbidden"));
            }
        }

        String filename = info.getFilename();
        String contentType = info.getContentType();
        return streamObject(info.getObjectName(), filename, contentType);
    }

    private ResponseEntity<InputStreamResource> streamObject(String objectName, String filename, String contentType) throws Exception {
        InputStream in = fileService.downloadFile(objectName);
        InputStreamResource resource = new InputStreamResource(in);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        builder.header(HttpHeaders.CACHE_CONTROL, "private, max-age=60");
        if (contentType != null && !contentType.isBlank()) {
            try {
                builder.contentType(MediaType.parseMediaType(contentType));
            } catch (Exception ex) {
                builder.contentType(MediaType.APPLICATION_OCTET_STREAM);
            }
        } else {
            builder.contentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        if (filename != null && !filename.isBlank()) {
            String safeFilename = filename.replaceAll("[\\r\\n\\\"]", "_");
            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(safeFilename, StandardCharsets.UTF_8)
                    .build();
            builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        }
        return builder.body(resource);
    }

    private CPFileInfo resolveFileInfo(String shareKey) {
        return fileInfoDao.getByShareKey(shareKey.trim());
    }

    private long parseLongId(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
    }

    private Long verifyBearerIfPresent(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!auth.startsWith(prefix)) {
            return null;
        }
        String token = auth.substring(prefix.length()).trim();
        return accessTokenService.verify(token);
    }
}
