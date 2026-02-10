package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
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
 * 文件 API 控制器。
 * <p>
 * 提供上传申请、上传完成回写与下载访问控制能力，并将访问失败统一映射为标准问题响应。
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

    /**
     * 构造文件控制器。
     *
     * @param flowRunner API 责任链执行器。
     * @param fileInfoDao 文件元信息数据访问对象。
     * @param channelMemberDao 频道成员数据访问对象。
     * @param fileTokenService 文件上传令牌服务。
     * @param fileService 文件对象存储服务。
     * @param accessTokenService Access Token 校验服务。
     */
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
     * 申请文件上传槽位并返回上传令牌。
     *
     * @param body 上传申请参数。
     * @param request HTTP 请求对象。
     * @return 责任链写入的标准响应数据。
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
     * 上传二进制文件内容并落库上传完成状态。
     *
     * @param fileId 文件 ID。
     * @param request HTTP 请求对象。
     * @return 无内容成功响应。
     * @throws Exception 当读取请求体或写入对象存储失败时抛出。
     */
    @PutMapping("/api/files/upload/{file_id}")
    public ResponseEntity<Void> upload(@PathVariable("file_id") String fileId,
                                       HttpServletRequest request) throws Exception {
        long uid = ApiAuth.requireUid(request);

        String token = request.getHeader("x-cp-upload-token");
        FileTokenService.FileToken ft = fileTokenService.consume(token);
        if (ft == null || !"UPLOAD".equalsIgnoreCase(ft.getOp()) || ft.getUid() != uid || ft.getFileId() == null || !ft.getFileId().equals(fileId)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.UNAUTHORIZED, "invalid upload token"));
        }

        long id = parseLongId(fileId);
        CPFileInfo info = fileInfoDao.getById(id);
        if (info == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.NOT_FOUND, "file not found"));
        }
        if (info.getOwnerUid() != uid) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
        }

        long expectedSize = info.getSize();
        long contentLength = request.getContentLengthLong();
        if (contentLength > 0 && contentLength != expectedSize) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }

        try (InputStream in = request.getInputStream()) {
            fileService.uploadIfNotExists(info.getObjectName(), in, expectedSize, info.getContentType());
        }

        info.setUploaded(true).setUploadedTime(TimeUtil.currentLocalDateTime());
        if (!fileInfoDao.save(info)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "failed to save file info"));
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * 按分享键下载文件。
     * <p>
     * 访问控制规则由 `access_scope` 与请求鉴权状态共同决定：
     * `PUBLIC` 允许匿名访问，`AUTH` 需要登录态，`OWNER` 仅上传者可见，`CHANNEL` 仅频道成员可见。
     *
     * @param shareKey 文件分享键。
     * @param request HTTP 请求对象。
     * @return 文件流响应。
     * @throws Exception 当文件读取失败时抛出。
     */
    @GetMapping("/api/files/download/{share_key}")
    public ResponseEntity<InputStreamResource> download(@PathVariable("share_key") String shareKey,
                                                        HttpServletRequest request) throws Exception {
        if (shareKey == null || shareKey.isBlank()) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        if ("server_avatar".equals(shareKey)) {
            return streamObject("server_avatar", null, null);
        }

        CPFileInfo info = resolveFileInfo(shareKey);
        if (info == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.NOT_FOUND, "file not found"));
        }
        if (!info.isUploaded()) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.NOT_FOUND, "file not found"));
        }

        CPFileAccessScopeEnum scope = info.getAccessScope() == null ? CPFileAccessScopeEnum.OWNER : info.getAccessScope();
        if (scope != CPFileAccessScopeEnum.PUBLIC) {
            Long uid = verifyBearerIfPresent(request);
            if (uid == null) {
                throw new CPProblemException(CPProblem.of(CPProblemReason.UNAUTHORIZED, "missing or invalid access token"));
            }
            if (scope == CPFileAccessScopeEnum.OWNER) {
                if (info.getOwnerUid() != uid) {
                    throw new CPProblemException(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
                }
            } else if (scope == CPFileAccessScopeEnum.CHANNEL) {
                long cid = info.getScopeCid();
                if (cid <= 0) {
                    throw new CPProblemException(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "invalid file scope"));
                }
                if (channelMemberDao.getMember(uid, cid) == null) {
                    throw new CPProblemException(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
                }
            } else if (scope == CPFileAccessScopeEnum.AUTH) {
            } else {
                throw new CPProblemException(CPProblem.of(CPProblemReason.FORBIDDEN, "forbidden"));
            }
        }

        String filename = info.getFilename();
        String contentType = info.getContentType();
        return streamObject(info.getObjectName(), filename, contentType);
    }

    /**
     * 从对象存储读取文件并构造下载响应头。
     *
     * @param objectName 对象存储键名。
     * @param filename 下载文件名。
     * @param contentType 文件 MIME 类型。
     * @return 包含输入流的 HTTP 响应。
     * @throws Exception 当对象存储读取失败时抛出。
     */
    private ResponseEntity<InputStreamResource> streamObject(String objectName,
                                                             String filename,
                                                             String contentType) throws Exception {
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

    /**
     * 根据分享键查询文件元信息。
     *
     * @param shareKey 文件分享键。
     * @return 文件元信息；不存在时返回 {@code null}。
     */
    private CPFileInfo resolveFileInfo(String shareKey) {
        return fileInfoDao.getByShareKey(shareKey.trim());
    }

    /**
     * 解析字符串 ID。
     *
     * @param str 待解析字符串。
     * @return 解析后的文件 ID。
     * @throws CPProblemException 当字符串不是合法整数时抛出。
     */
    private long parseLongId(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
    }

    /**
     * 在请求携带 Bearer Token 时执行校验并返回用户 ID。
     *
     * @param request HTTP 请求对象。
     * @return 用户 ID；未携带或校验失败时返回 {@code null}。
     */
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
