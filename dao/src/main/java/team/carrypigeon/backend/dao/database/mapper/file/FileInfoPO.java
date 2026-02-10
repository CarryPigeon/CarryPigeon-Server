package team.carrypigeon.backend.dao.database.mapper.file;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.file.CPFileAccessScopeEnum;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;

import java.time.LocalDateTime;

/**
 * `file_info` 表持久化对象。
 * <p>
 * 负责数据库字段与领域对象 {@link CPFileInfo} 之间的转换。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("file_info")
public class FileInfoPO {

    /** 主键 ID。 */
    @TableId
    private Long id;

    /** 对外共享键。 */
    private String shareKey;

    /** 上传者用户 ID。 */
    private Long ownerUid;

    /**
     * 访问范围（数据库中存储为大写字符串）。
     */
    private String accessScope;

    /** 访问范围绑定频道 ID。 */
    private Long scopeCid;

    /** 访问范围绑定消息 ID。 */
    private Long scopeMid;

    /** 原始文件名。 */
    private String filename;

    /** SHA-256 摘要。 */
    private String sha256;

    /** 文件大小（字节）。 */
    private Long size;

    /** 对象存储对象名。 */
    private String objectName;

    /** MIME 类型。 */
    private String contentType;

    /** 是否已完成上传。 */
    private Boolean uploaded;

    /** 上传完成时间。 */
    private LocalDateTime uploadedTime;

    /** 记录创建时间。 */
    private LocalDateTime createTime;

    /**
     * 将当前 PO 转换为领域对象。
     */
    public CPFileInfo toBo() {
        return new CPFileInfo()
                .setId(id)
                .setShareKey(shareKey)
                .setOwnerUid(ownerUid == null ? 0L : ownerUid)
                .setAccessScope(CPFileAccessScopeEnum.parseOrDefault(accessScope))
                .setScopeCid(scopeCid == null ? 0L : scopeCid)
                .setScopeMid(scopeMid == null ? 0L : scopeMid)
                .setFilename(filename)
                .setSha256(sha256)
                .setSize(size)
                .setObjectName(objectName)
                .setContentType(contentType)
                .setUploaded(uploaded != null && uploaded)
                .setUploadedTime(uploadedTime)
                .setCreateTime(createTime);
    }

    /**
     * 从领域对象构造 PO。
     */
    public static FileInfoPO fromBo(CPFileInfo info) {
        return new FileInfoPO()
                .setId(info.getId())
                .setShareKey(info.getShareKey())
                .setOwnerUid(info.getOwnerUid())
                .setAccessScope(info.getAccessScope() == null ? CPFileAccessScopeEnum.OWNER.name() : info.getAccessScope().name())
                .setScopeCid(info.getScopeCid())
                .setScopeMid(info.getScopeMid())
                .setFilename(info.getFilename())
                .setSha256(info.getSha256())
                .setSize(info.getSize())
                .setObjectName(info.getObjectName())
                .setContentType(info.getContentType())
                .setUploaded(info.isUploaded())
                .setUploadedTime(info.getUploadedTime())
                .setCreateTime(info.getCreateTime());
    }
}
