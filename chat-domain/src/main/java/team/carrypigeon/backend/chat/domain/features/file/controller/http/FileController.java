package team.carrypigeon.backend.chat.domain.features.file.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.dto.CreateFileUploadRequest;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.dto.FileUploadResponse;
import team.carrypigeon.backend.chat.domain.features.file.config.ServerAvatarProperty;
import team.carrypigeon.backend.chat.domain.features.file.domain.problem.FileProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 文件 HTTP 入口。
 * 职责：提供上传申请、同源上传和下载访问能力。
 * 边界：当前不引入独立文件表，仅依赖 share_key 到对象键的稳定映射规则。
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件", description = "文件上传申请与下载能力。")
public class FileController {

    private final FileTransferApi fileTransferDomainApi;
    private final ServerAvatarProperty serverAvatarProperty;

    /**
     * 创建文件 HTTP 入口。
     *
     * @param fileTransferDomainApi 文件传输领域 API
     * @param serverAvatarProperty 服务端头像配置
     */
    public FileController(FileTransferApi fileTransferDomainApi, ServerAvatarProperty serverAvatarProperty) {
        this.fileTransferDomainApi = fileTransferDomainApi;
        this.serverAvatarProperty = serverAvatarProperty;
    }

    /**
     * 申请文件上传授权。
     * 输入：文件名、MIME 类型和声明大小。
     * 输出：包含 `share_key`、上传地址和过期时间的上传响应。
     *
     * @param request 上传申请请求
     * @param principal 当前认证主体
     * @return 文件上传授权响应
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/uploads")
    @Operation(summary = "申请文件上传", description = "生成同源上传入口与稳定 share_key。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回上传授权结果")
    })
    public FileUploadResponse createUpload(
        @Valid @RequestBody CreateFileUploadRequest request,
        @AuthenticationPrincipal CpPrincipal principal
    ) {
        FileUploadGrantResult result = fileTransferDomainApi.createUploadGrant(
                principal.accountId(),
                request.sizeBytes()
        );
        return new FileUploadResponse(
                IdUtil.toString(result.fileId()),
                result.shareKey(),
                new FileUploadResponse.UploadResponse(
                        "PUT",
                        result.uploadUrl(),
                        java.util.Map.of(),
                        result.expiresAt().toEpochMilli()
                )
        );
    }

    /**
     * 通过同源 HTTP PUT 写入文件内容。
     * 副作用：会把请求体内容写入对象存储。
     * 失败：当请求体读取失败时返回统一业务失败异常。
     *
     * @param shareKey 上传授权 share key
     * @param request 当前 HTTP 请求，提供认证主体和上传内容流
     * @return HTTP 204
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping(path = "/uploads/{shareKey}", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "写入文件内容", description = "使用上传授权 share_key 写入原始文件内容。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "文件内容写入成功")})
    public ResponseEntity<Void> uploadFile(
        @PathVariable String shareKey,
        @AuthenticationPrincipal CpPrincipal principal,
        HttpServletRequest request
    ) {
        if (request.getContentType() == null) {
            throw ProblemException.of(FileProblem.INVALID_INPUT, "file content must not be null");
        }
        try {
            fileTransferDomainApi.uploadFile(
                    principal.accountId(),
                    shareKey,
                    request.getContentType(),
                    request.getContentLengthLong(),
                    request.getInputStream()
            );
            return ResponseEntity.noContent().build();
        } catch (IOException exception) {
            throw ProblemException.of(FileProblem.UPLOAD_READ_FAILED, exception);
        }
    }

    /**
     * 按 `share_key` 下载文件。
     * 输出：通过应用服务器转发文件内容流。
     *
     * @param shareKey 下载 share key
     * @return 文件内容响应
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download/{shareKey}")
    @Operation(summary = "获取文件下载", description = "按 share_key 返回文件内容。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "直接返回文件二进制内容",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public ResponseEntity<?> download(
        @PathVariable String shareKey
    ) {
        FileDownloadResult downloadResult = fileTransferDomainApi.downloadFile(shareKey)
                .orElseThrow(() -> ProblemException.of(FileProblem.FILE_NOT_FOUND));
        String type = downloadResult.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : downloadResult.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, type)
                .header(HttpHeaders.CONTENT_LENGTH, Long.toString(downloadResult.size()))
                .body(new InputStreamResource(downloadResult.content()));
    }

    /**
     * 下载服务端头像。
     * 输入：无，匿名可访问。
     * 输出：从本地配置路径读取服务端头像文件内容。
     * 失败语义：头像文件不存在时返回 404。
     *
     * @return 服务端头像二进制内容
     */
    @PermitAll
    @GetMapping("/download/server/head")
    @Operation(summary = "下载服务端头像", description = "从本地配置路径读取并返回服务端头像。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回服务端头像",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "服务端头像不存在")
    })
    public ResponseEntity<?> downloadServerHead() {
        Path avatarPath = Path.of(serverAvatarProperty.path());
        if (!Files.exists(avatarPath)) {
            throw ProblemException.of(FileProblem.FILE_NOT_FOUND, "server avatar file not found: " + serverAvatarProperty.path());
        }
        try {
            InputStream inputStream = Files.newInputStream(avatarPath);
            String contentType = Files.probeContentType(avatarPath);
            String type = contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, type)
                    .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            throw ProblemException.of(FileProblem.UPLOAD_READ_FAILED, exception);
        }
    }
}
