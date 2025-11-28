package team.carrypigeon.backend.chat.domain.controller.web.file;

import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import team.carrypigeon.backend.chat.domain.service.file.FileService;
import team.carrypigeon.backend.chat.domain.service.file.FileTokenService;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.io.InputStream;
import java.security.MessageDigest;

@RestController
@Slf4j
@RequestMapping("/file")
public class MinioController {

    private final FileService fileService;
    private final FileTokenService fileTokenService;

    public MinioController(FileService fileService, FileTokenService fileTokenService) {
        this.fileService = fileService;
        this.fileTokenService = fileTokenService;
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
            // compute sha256 and size
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            long size = 0L;
            try (InputStream digestInputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = digestInputStream.read(buffer)) != -1) {
                    sha256Digest.update(buffer, 0, bytesRead);
                    size += bytesRead;
                }
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
            String objectName = sha256;
            String contentType = file.getContentType();
            try (InputStream uploadStream = file.getInputStream()) {
                boolean uploaded = fileService.uploadIfNotExists(objectName, uploadStream, size, contentType);
                log.info("file upload, token={}, uid={}, objectName={}, uploaded={}, size={}, sha256={}",
                        token, fileToken.getUid(), objectName, uploaded, size, sha256);
            }
            // return the sha256 as file id
            return ResponseEntity.ok(sha256);
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
        String objectName = fileToken.getFileId();
        if (objectName == null || objectName.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + objectName)
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
