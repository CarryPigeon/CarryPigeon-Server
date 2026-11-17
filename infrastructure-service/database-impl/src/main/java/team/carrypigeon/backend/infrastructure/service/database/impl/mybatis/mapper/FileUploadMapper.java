package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.time.Instant;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileStorageUsageRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileUploadRecord;

/** 文件状态 SQL；每次上传使用不同的对象键，旧尝试单独保留供后台回收。 */
@Mapper
public interface FileUploadMapper {
    /** 并发首次申请依靠唯一键串行创建配额行，不把额度计数保存在内存中。 */
    @Insert("""
            INSERT INTO chat_file_quota (account_id) VALUES (#{accountId})
            ON DUPLICATE KEY UPDATE account_id = VALUES(account_id)
            """)
    int ensureQuotaAccount(@Param("accountId") long accountId);

    @Select("SELECT account_id FROM chat_file_quota WHERE account_id = #{accountId} FOR UPDATE")
    long lockQuotaAccount(@Param("accountId") long accountId);

    @Select("""
            SELECT COALESCE(SUM(size_bytes), 0) AS allocated_bytes,
                   COALESCE(SUM(status != 'COMPLETED'), 0) AS unfinished_uploads
            FROM chat_file_upload
            WHERE owner_account_id = #{accountId}
              AND (status IN ('COMPLETED', 'UPLOADING') OR (status = 'PENDING' AND expires_at > #{now}))
            """)
    FileStorageUsageRecord storageUsage(@Param("accountId") long accountId, @Param("now") Instant now);

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
            WHERE file_id = #{fileId} AND status IN ('PENDING', 'FAILED') AND expires_at > #{now}
            """)
    int claim(@Param("fileId") long fileId, @Param("objectKey") String objectKey, @Param("now") Instant now);

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
