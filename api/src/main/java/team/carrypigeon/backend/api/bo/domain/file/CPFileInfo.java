package team.carrypigeon.backend.api.bo.domain.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文件元数据领域对象。
 * <p>
 * 该对象描述文件在系统中的业务属性，不直接包含二进制内容。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPFileInfo {

    /**
     * 文件 ID（全局唯一，通常由雪花算法生成）。
     */
    private long id;

    /**
     * 对外共享键。
     * <p>
     * 客户端通过该键访问 `GET /api/files/download/{share_key}`。
     */
    private String shareKey;

    /**
     * 上传者用户 ID。
     */
    private long ownerUid;

    /**
     * 下载访问范围。
     */
    private CPFileAccessScopeEnum accessScope;

    /**
     * 访问范围绑定频道 ID。
     * <p>
     * 当 `accessScope=CHANNEL` 时应大于 0。
     */
    private long scopeCid;

    /**
     * 访问范围绑定消息 ID（预留扩展）。
     */
    private long scopeMid;

    /**
     * 原始文件名（可为空）。
     */
    private String filename;

    /**
     * 文件内容 SHA-256（十六进制字符串）。
     */
    private String sha256;

    /**
     * 文件大小（字节）。
     */
    private long size;

    /**
     * 对象存储中的对象名。
     */
    private String objectName;

    /**
     * MIME 类型（可为空）。
     */
    private String contentType;

    /**
     * 是否已完成二进制上传。
     */
    private boolean uploaded;

    /**
     * 上传完成时间（未上传时为空）。
     */
    private LocalDateTime uploadedTime;

    /**
     * 元数据创建时间。
     */
    private LocalDateTime createTime;
}
