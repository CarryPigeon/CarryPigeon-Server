package team.carrypigeon.backend.chat.domain.features.file.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.file.application.dto.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.application.service.FileApplicationService;
import team.carrypigeon.backend.chat.domain.features.file.controller.dto.CreateFileUploadRequest;
import team.carrypigeon.backend.chat.domain.features.file.controller.dto.FileUploadResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;

/**
 * 文件 HTTP 入口。
 * 职责：提供上传申请、同源上传和下载访问能力。
 * 边界：当前不引入独立文件表，仅依赖 share_key 到对象键的稳定映射规则。
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件", description = "文件上传申请与下载能力。")
public class FileController {

    private final FileApplicationService fileApplicationService;
    private final AuthRequestContext authRequestContext;

    public FileController(FileApplicationService fileApplicationService, AuthRequestContext authRequestContext) {
        this.fileApplicationService = fileApplicationService;
        this.authRequestContext = authRequestContext;
    }

    @PostMapping("/uploads")
    @Operation(summary = "申请文件上传", description = "生成同源上传入口与稳定 share_key。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回上传授权结果")
    })
    /**
     * 申请文件上传授权。
     * 输入：文件名、MIME 类型和声明大小。
     * 输出：包含 `share_key`、上传地址和过期时间的上传响应。
     */
    public FileUploadResponse createUpload(@Valid @RequestBody CreateFileUploadRequest request, HttpServletRequest servletRequest) {
        authRequestContext.requirePrincipal(servletRequest);
        FileUploadGrantResult result = fileApplicationService.createUploadGrant(request.filename(), request.mimeType(), request.sizeBytes());
        return new FileUploadResponse(
                Ids.toString(result.fileId()),
                result.shareKey(),
                new FileUploadResponse.UploadResponse(
                        "PUT",
                        result.uploadUrl(),
                        fileApplicationService.uploadHeaders(),
                        result.expiresAt().toEpochMilli()
                )
        );
    }

    /**
     * 通过同源 HTTP PUT 写入文件内容。
     * 副作用：会把请求体内容写入对象存储。
     * 失败：当请求体读取失败时返回统一业务失败异常。
     */
    @PutMapping(path = "/uploads/{shareKey}", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> uploadFile(@PathVariable String shareKey, HttpServletRequest request) {
        authRequestContext.requirePrincipal(request);
        try {
            fileApplicationService.uploadFile(shareKey, request.getContentType(), request.getContentLengthLong(), request.getInputStream());
            return ResponseEntity.noContent().build();
        } catch (IOException exception) {
            throw ProblemException.fail("file_upload_read_failed", "failed to read upload content");
        }
    }

    @GetMapping("/download/{shareKey}")
    @Operation(summary = "获取文件下载", description = "按 share_key 返回下载入口。")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "重定向到对象下载地址"),
            @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    /**
     * 按 `share_key` 下载文件。
     * 约束：普通文件要求登录；服务端头像允许匿名访问。
     * 输出：对象带内容流时直接返回二进制，否则重定向到预签名地址。
     */
    public ResponseEntity<?> download(@PathVariable String shareKey, HttpServletRequest request) {
        if (!fileApplicationService.isServerAvatar(shareKey)) {
            authRequestContext.requirePrincipal(request);
        }
        StorageObject storageObject = fileApplicationService.findStorageObject(shareKey)
                .orElseThrow(() -> ProblemException.notFound("file does not exist"));
        if (storageObject.content().isPresent()) {
            byte[] content = readContent(storageObject);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, storageObject.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : storageObject.contentType())
                    .header(HttpHeaders.CONTENT_LENGTH, Long.toString(content.length))
                    .body(content);
        }
        var presignedUrl = fileApplicationService.createDownloadUrl(shareKey);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, presignedUrl.url().toString())
                .header(HttpHeaders.CONTENT_TYPE, storageObject.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : storageObject.contentType())
                .build();
    }

    private byte[] readContent(StorageObject storageObject) {
        try (var inputStream = storageObject.content().orElseThrow();
             var outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw ProblemException.fail("file_download_read_failed", "failed to read file content");
        }
    }
}
