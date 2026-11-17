package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileUploadRecord;

/** 文件状态 SQL；每次上传使用不同的对象键，旧尝试单独保留供后台回收。 */
@Mapper
public interface FileUploadMapper {
    @Insert("""
            INSERT INTO chat_file_upload (file_id, owner_account_id, share_key, size_bytes, status, expires_at)
            VALUES (#{fileId}, #{ownerAccountId}, #{shareKey}, #{sizeBytes}, #{status}, #{expiresAt})
            """)
    int insert(FileUploadRecord upload);

    @Select("""
            SELECT file_id, owner_account_id, share_key, size_bytes, status, object_key, sha256,
                   content_type, expires_at, completed_at
            FROM chat_file_upload WHERE share_key = #{shareKey}
            """)
    FileUploadRecord selectByShareKey(@Param("shareKey") String shareKey);

    @Update("""
            UPDATE chat_file_upload SET status = 'UPLOADING', object_key = #{objectKey}
            WHERE file_id = #{fileId} AND status IN ('PENDING', 'FAILED')
            """)
    int claim(@Param("fileId") long fileId, @Param("objectKey") String objectKey);

    @Insert("""
            INSERT INTO chat_file_upload_attempt (object_key, file_id) VALUES (#{objectKey}, #{fileId})
            """)
    int insertAttempt(@Param("fileId") long fileId, @Param("objectKey") String objectKey);

    @Update("""
            UPDATE chat_file_upload SET status = 'COMPLETED', sha256 = #{sha256},
                content_type = #{contentType}, completed_at = #{completedAt}
            WHERE file_id = #{fileId} AND status = 'UPLOADING' AND object_key = #{objectKey}
            """)
    int complete(@Param("fileId") long fileId, @Param("objectKey") String objectKey,
            @Param("sha256") String sha256, @Param("contentType") String contentType,
            @Param("completedAt") Instant completedAt);

    @Update("""
            UPDATE chat_file_upload SET status = 'FAILED'
            WHERE file_id = #{fileId} AND status = 'UPLOADING' AND object_key = #{objectKey}
            """)
    int fail(@Param("fileId") long fileId, @Param("objectKey") String objectKey);
}
