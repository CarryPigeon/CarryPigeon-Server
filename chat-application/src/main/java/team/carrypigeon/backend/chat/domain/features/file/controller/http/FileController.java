package team.carrypigeon.backend.chat.domain.features.file.controller.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.file.config.ServerAvatarProperty;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.dto.CreateFileUploadRequest;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.dto.FileUploadResponse;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.problem.FileProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 文件 HTTP 入口。
 * 职责：提供上传申请、同源上传和下载访问能力。
 * 边界：上传状态与文件发布由领域服务负责，本入口只处理 HTTP 内容流。
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
     * @param serverAvatarProperty  服务端头像配置
     */
    public FileController(FileTransferApi fileTransferDomainApi, ServerAvatarProperty serverAvatarProperty) {
        this.fileTransferDomainApi = fileTransferDomainApi;
        this.serverAvatarProperty = serverAvatarProperty;
    }

    /**
     * 申请文件上传授权。
     *
     * <p>请求 {@code POST /api/files/uploads}。JSON 请求体示例 {@code {"filename":"note.txt","size_bytes":3,"sha256":"<64-hex>"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code FileUploadResponse}。
     * <p>失败示例：声明文件过大 40001，sha256 缺失 10001。
     *
     * @param request   上传申请请求
     * @param principal 当前认证主体
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/uploads", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "申请文件上传", description = "生成同源上传入口与稳定 share_key。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回上传授权结果", useReturnTypeSchema = true))
    public Result createUpload(
        @Valid
        @RequestBody
        CreateFileUploadRequest request,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        FileUploadGrantResult result = fileTransferDomainApi.createUploadGrant(
            principal.accountId(),
            request.filename(),
            request.sha256(),
            request.sizeBytes()
        );
        return Result.success(
            new FileUploadResponse(
                result.fileId(),
                result.shareKey(),
                new FileUploadResponse.UploadResponse(
                    "PUT",
                    result.uploadUrl(),
                    Map.of(HttpHeaders.CONTENT_LENGTH, Long.toString(request.sizeBytes())),
                    result.expiresAt().toEpochMilli(),
                    result.hasSameFile()
                )
            )
        );
    }

    /**
     * 通过同源 HTTP PUT 写入文件内容。
     * 副作用：会把请求体内容写入对象存储。
     *
     * <p>请求 {@code PUT /api/files/uploads/example-share-key}。请求体为原始字节，例如 text/plain 的 abc；Content-Length 为 3。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：缺少长度头 HTTP 411，请求体过大 HTTP 413，授权无效 40002。
     *
     * @param shareKey 上传授权 share key
     * @param request  当前 HTTP 请求，提供认证主体和上传内容流
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping(path = "/uploads/{shareKey}", consumes = MediaType.ALL_VALUE, produces = "application/json")
    @Operation(summary = "写入文件内容", description = "使用上传授权 share_key 写入原始文件内容；完成后相同内容重试返回成功，不同内容返回业务码 40006。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result uploadFile(
        @PathVariable
        String shareKey,
        @AuthenticationPrincipal
        CpPrincipal principal,
        HttpServletRequest request
    ) {
        // 不接受未知长度的分块上传；实际读取仍按授权大小校验，不能只信任此头。
        if (request.getContentLengthLong() < 0) {
            throw ProblemException.of(FileProblem.LENGTH_REQUIRED);
        }
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
            return Result.success();
        } catch (IOException exception) {
            throw ProblemException.of(FileProblem.UPLOAD_READ_FAILED, exception);
        }
    }

    /**
     * 按 `share_key` 下载已完成文件。
     * 访问规则：登录且持有链接即可下载，不检查频道成员关系；上传授权到期不影响下载。
     *
     * <p>请求 {@code GET /api/files/download/example-share-key}。无请求体。
     * <p>成功响应 HTTP 200，返回文件原始字节及对应媒体类型。
     * <p>失败示例：文件不存在 HTTP 404，缺少认证 HTTP 401。
     *
     * @param shareKey 下载 share key
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download/{shareKey}")
    @Operation(summary = "获取文件下载", description = "任意已认证用户持有 share_key 即可下载已完成文件，不要求频道成员身份；上传授权到期不影响下载。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "直接返回文件二进制内容",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "尚未认证"),
        @ApiResponse(responseCode = "404", description = "文件不存在、尚未完成或已回收")
    })
    public ResponseEntity<InputStreamResource> download(
        @PathVariable
        String shareKey
    ) {
        FileDownloadResult downloadResult = fileTransferDomainApi.downloadFile(shareKey)
            .orElseThrow(() -> ProblemException.of(FileProblem.FILE_NOT_FOUND));
        String type = downloadResult.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : downloadResult.contentType();
        String contentDisposition = ContentDisposition.attachment()
            .filename(downloadResult.fileName(), StandardCharsets.UTF_8)
            .build()
            .toString();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, type)
            .header(HttpHeaders.CONTENT_LENGTH, Long.toString(downloadResult.size()))
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition) // 关键：下载文件名
            .body(new InputStreamResource(downloadResult.content()));
    }

    /**
     * 下载服务端头像。
     *
     * <p>请求 {@code GET /api/files/download/server/head}。无请求体。
     * <p>成功响应 HTTP 200，返回文件原始字节及对应媒体类型。
     * <p>失败示例：文件不存在 HTTP 404。
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
    public ResponseEntity<InputStreamResource> downloadServerHead() {
        Path avatarPath = Path.of(serverAvatarProperty.path());
        if (!Files.exists(avatarPath)) {
            throw ProblemException.of(FileProblem.FILE_NOT_FOUND, "server avatar file not found");
        }
        try {
            String contentType = Files.probeContentType(avatarPath);
            InputStream inputStream = Files.newInputStream(avatarPath);
            String type = contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, type)
                .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            throw ProblemException.of(FileProblem.UPLOAD_READ_FAILED, exception);
        }
    }
}
