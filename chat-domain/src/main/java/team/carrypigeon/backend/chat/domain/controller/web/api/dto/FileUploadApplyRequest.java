package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 文件上传申请请求体。
 *
 * @param filename 文件名。
 * @param mimeType MIME 类型。
 * @param sizeBytes 文件大小（字节）。
 * @param sha256 文件 SHA-256 摘要。
 * @param scope 下载访问范围（`OWNER|AUTH|CHANNEL|PUBLIC`）。
 * @param scopeCid 当 `scope=CHANNEL` 时对应的频道 ID。
 */
public record FileUploadApplyRequest(@NotBlank String filename,
                                     String mimeType,
                                     @NotNull @Min(1) Long sizeBytes,
                                     String sha256,
                                     String scope,
                                     String scopeCid) {
}
