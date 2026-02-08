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
 * Persistence object for file metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("file_info")
public class FileInfoPO {

    @TableId
    private Long id;

    private String shareKey;

    private Long ownerUid;

    /**
     * Access scope in DB (uppercase string).
     */
    private String accessScope;

    private Long scopeCid;

    private Long scopeMid;

    private String filename;

    private String sha256;

    private Long size;

    private String objectName;

    private String contentType;

    private Boolean uploaded;

    private LocalDateTime uploadedTime;

    private LocalDateTime createTime;

    /**
     * 将当前 PO 转换为领域对象（BO）。
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
     * 从领域对象（BO）创建 PO。
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
