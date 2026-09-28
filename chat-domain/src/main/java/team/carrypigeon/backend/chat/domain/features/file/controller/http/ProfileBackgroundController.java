package team.carrypigeon.backend.chat.domain.features.file.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.file.controller.dto.ProfileBackgroundUploadResponse;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 用户资料背景图 HTTP 入口。
 * 职责：保持用户背景图上传协议，并将文件读写委托给 file 领域 API。
 */
@RestController
@PreAuthorize("isAuthenticated()")
public class ProfileBackgroundController {

    private final FileTransferApi fileTransferApi;

    public ProfileBackgroundController(
            FileTransferApi fileTransferApi
    ) {
        this.fileTransferApi = fileTransferApi;
    }

    @PostMapping(path = "/api/users/me/background", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "更新当前用户背景图", description = "上传当前用户背景图并返回下载地址。")
    public ProfileBackgroundUploadResponse upload(
        @AuthenticationPrincipal CpPrincipal cpPrincipal,
        @RequestPart("background") MultipartFile background
    ) {
        try {
            String shareKey = fileTransferApi.uploadProfileBackground(
                cpPrincipal.accountId(),
                background.getContentType(),
                background.getSize(),
                background.getInputStream()
            );
            return new ProfileBackgroundUploadResponse("/api/files/download/" + shareKey);
        } catch (IOException exception) {
            throw ProblemException.fail("background_upload_read_failed", "failed to read background upload content");
        }
    }
}
