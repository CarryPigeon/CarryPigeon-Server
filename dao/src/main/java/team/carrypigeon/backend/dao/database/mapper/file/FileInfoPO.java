package team.carrypigeon.backend.dao.database.mapper.file;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
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

    private String sha256;

    private Long size;

    private String objectName;

    private String contentType;

    private LocalDateTime createTime;

    public CPFileInfo toBo() {
        return new CPFileInfo()
                .setId(id)
                .setSha256(sha256)
                .setSize(size)
                .setObjectName(objectName)
                .setContentType(contentType)
                .setCreateTime(createTime);
    }

    public static FileInfoPO fromBo(CPFileInfo info) {
        return new FileInfoPO()
                .setId(info.getId())
                .setSha256(info.getSha256())
                .setSize(info.getSize())
                .setObjectName(info.getObjectName())
                .setContentType(info.getContentType())
                .setCreateTime(info.getCreateTime());
    }
}
