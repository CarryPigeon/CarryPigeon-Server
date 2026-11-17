package team.carrypigeon.backend.chat.domain.features.file.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** 账号文件配额：字节数包含已完成文件与未完成授权的预留，不限制每日上传量。 */
@Validated
@ConfigurationProperties(prefix = "cp.chat.file.upload")
public record FileUploadProperties(
        @DefaultValue("10737418240") @Min(1) long accountQuotaBytes,
        @DefaultValue("5") @Min(1) int maxUnfinishedUploads,
        @DefaultValue("524288000") @Min(1) long maxFileBytes
) {}
