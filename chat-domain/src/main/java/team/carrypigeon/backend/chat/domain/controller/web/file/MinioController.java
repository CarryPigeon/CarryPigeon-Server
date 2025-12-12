package team.carrypigeon.backend.chat.domain.controller.web.file;

import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.service.file.FileService;
import team.carrypigeon.backend.chat.domain.service.file.FileTokenService;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

@RestController
@Slf4j
@RequestMapping("/file")
public class MinioController {

    private final FileService fileService;
    private final FileTokenService fileTokenService;
    private final FileInfoDao fileInfoDao;

    public MinioController(FileService fileService, FileTokenService fileTokenService, FileInfoDao fileInfoDao) {
        this.fileService = fileService;
        this.fileTokenService = fileTokenService;
        this.fileInfoDao = fileInfoDao;
    }

    /**
     * Upload file with one-time token.
     * Token should be obtained via Netty channel beforehand.
     */
    @PostMapping("/upload/{token}")
    public ResponseEntity<?> upload(@PathVariable("token") String token,
                                    @RequestParam("file") MultipartFile file) {
        FileTokenService.FileToken fileToken = fileTokenService.consume(token);
        if (fileToken == null || fileToken.getOp() == null || !"UPLOAD".equalsIgnoreCase(fileToken.getOp())) {
            return ResponseEntity.status(403).body("invalid or expired token");
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("file is empty");
        }
        try {
            // 生成文件ID，采用全局雪花ID
            long fileId = IdUtil.generateId();
            String fileIdStr = Long.toString(fileId);
            String objectName = fileIdStr;
            String contentType = file.getContentType();
            long size = file.getSize();
            if (size <= 0) {
                // 极端情况下 size 不可用时，退化为两遍读取：第一遍统计长度
                long counted = 0L;
                try (InputStream in = file.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        counted += bytesRead;
                    }
                }
                size = counted;
            }

            // 边上传边计算 sha256
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            try (InputStream uploadStream = new DigestInputStream(file.getInputStream(), sha256Digest)) {
                fileService.upload(objectName, uploadStream, size, contentType);
            }
            byte[] digest = sha256Digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            String sha256 = hex.toString();

            // 上传完成后，通过 sha256 + size 在数据库中检查是否已有文件
            CPFileInfo existing = fileInfoDao.getBySha256AndSize(sha256, size);
            if (existing != null) {
                // 已存在相同文件：删除刚上传的对象，返回已有 fileId
                try {
                    fileService.deleteFile(objectName);
                    log.info("duplicate file detected, delete newly uploaded object. newObjectName={}, existingFileId={}",
                            objectName, existing.getId());
                } catch (Exception e) {
                    log.warn("failed to delete duplicated object from minio, objectName={}", objectName, e);
                }
                return ResponseEntity.ok(Long.toString(existing.getId()));
            }

            // 不存在则插入新的文件信息记录
            CPFileInfo info = new CPFileInfo()
                    .setId(fileId)
                    .setSha256(sha256)
                    .setSize(size)
                    .setObjectName(objectName)
                    .setContentType(contentType)
                    .setCreateTime(TimeUtil.getCurrentLocalTime());
            boolean saved = fileInfoDao.save(info);
            if (!saved) {
                log.error("failed to save file info, fileId={}", fileId);
                return ResponseEntity.status(500).body("save file info fail");
            }
            // 返回新的 fileId
            return ResponseEntity.ok(fileIdStr);
        } catch (Exception e) {
            log.error("file upload error", e);
            return ResponseEntity.status(500).body("upload fail");
        }
    }

    /**
     * Download file by token (for large or sensitive files).
     */
    @GetMapping("/download/{token}")
    public ResponseEntity<StreamingResponseBody> downloadWithToken(@PathVariable("token") String token) {
        FileTokenService.FileToken fileToken = fileTokenService.consume(token);
        if (fileToken == null || fileToken.getOp() == null || !"DOWNLOAD".equalsIgnoreCase(fileToken.getOp())) {
            return ResponseEntity.status(403).build();
        }
        String fileId = fileToken.getFileId();
        if (fileId == null || fileId.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        String objectName = fileId;
        try {
            StatObjectResponse stat = fileService.statFile(objectName);
            String contentType = stat.contentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            String finalContentType = contentType;
            StreamingResponseBody body = outputStream -> {
                try (InputStream inputStream = fileService.downloadFile(objectName)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } catch (Exception e) {
                    log.error("file download with token error", e);
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileId)
                    .contentType(MediaType.parseMediaType(finalContentType))
                    .body(body);
        } catch (Exception e) {
            log.error("file download with token error", e);
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Direct download without token.
     * Only allowed when file size <= 3MB and content type is image/*.
     */
    @GetMapping("/raw/{fileId}")
    public ResponseEntity<StreamingResponseBody> downloadRaw(@PathVariable("fileId") String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            StatObjectResponse stat = fileService.statFile(fileId);
            long size = stat.size();
            String contentType = stat.contentType();
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            long maxSize = 3L * 1024 * 1024; // 3MB
            boolean isImage = contentType.toLowerCase().startsWith("image/");
            if (size > maxSize || !isImage) {
                // need token for large or non-image files
                return ResponseEntity.status(403).build();
            }
            String finalContentType = contentType;
            StreamingResponseBody body = outputStream -> {
                try (InputStream inputStream = fileService.downloadFile(fileId)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } catch (Exception e) {
                    log.error("file raw download error", e);
                }
            };
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(finalContentType))
                    .body(body);
        } catch (Exception e) {
            log.error("file raw download error", e);
            return ResponseEntity.status(404).build();
        }
    }
}
