package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileStorageUsageRecord;
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
                   COALESCE(SUM(status IN ('PENDING', 'UPLOADING')), 0) AS unfinished_uploads
            FROM chat_file_upload
            WHERE owner_account_id = #{accountId}
              AND (status IN ('COMPLETED', 'UPLOADING')
                   OR (status = 'DELETING' AND completed_at IS NOT NULL)
                   OR (status = 'PENDING' AND expires_at > #{now}))
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
            INSERT INTO chat_file_upload_attempt (object_key, file_id, created_at, next_cleanup_at)
            VALUES (#{objectKey}, #{fileId}, #{now}, NULL)
            """)
    int insertAttempt(@Param("fileId") long fileId, @Param("objectKey") String objectKey, @Param("now") Instant now);

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

    @Select("""
            SELECT file_id, owner_account_id, share_key, size_bytes, status, object_key, sha256,
                   content_type, expires_at, completed_at
            FROM chat_file_upload WHERE file_id = #{fileId} FOR UPDATE
            """)
    FileUploadRecord selectByIdForUpdate(@Param("fileId") long fileId);

    @Select("SELECT file_id FROM chat_file_avatar_reference WHERE account_id = #{accountId}")
    Long avatarFileId(@Param("accountId") long accountId);

    @Delete("DELETE FROM chat_file_avatar_reference WHERE account_id = #{accountId}")
    int deleteAvatarReference(@Param("accountId") long accountId);

    @Insert("INSERT INTO chat_file_avatar_reference (account_id, file_id) VALUES (#{accountId}, #{fileId})")
    int insertAvatarReference(@Param("accountId") long accountId, @Param("fileId") long fileId);

    @Update("""
            UPDATE chat_file_upload f SET status = 'DELETING', reclaim_started_at = #{now}
            WHERE file_id = #{fileId} AND status = 'COMPLETED'
              AND NOT EXISTS (SELECT 1 FROM chat_file_avatar_reference r WHERE r.file_id = f.file_id)
            """)
    int reclaimIfUnreferenced(@Param("fileId") long fileId, @Param("now") Instant now);

    @Select("""
            SELECT f.file_id FROM chat_file_upload f
            WHERE ((status IN ('PENDING', 'FAILED') AND expires_at <= #{now})
                OR (status = 'UPLOADING' AND expires_at <= #{interruptedBefore})
                OR (status = 'COMPLETED' AND completed_at <= #{unusedBefore}
                    AND NOT EXISTS (SELECT 1 FROM chat_file_avatar_reference r WHERE r.file_id = f.file_id)))
            ORDER BY file_id LIMIT 1 FOR UPDATE SKIP LOCKED
            """)
    Long reclaimCandidate(@Param("now") Instant now, @Param("unusedBefore") Instant unusedBefore,
            @Param("interruptedBefore") Instant interruptedBefore);

    @Update("""
            UPDATE chat_file_upload SET status = 'DELETING', reclaim_started_at = #{now} WHERE file_id = #{fileId}
            """)
    int beginReclaim(@Param("fileId") long fileId, @Param("now") Instant now);

    @Update("UPDATE chat_file_upload_attempt SET next_cleanup_at = #{now} WHERE file_id = #{fileId}")
    int scheduleObjects(@Param("fileId") long fileId, @Param("now") Instant now);

    @Select("""
            SELECT object_key FROM chat_file_upload_attempt
            WHERE next_cleanup_at <= #{now}
            ORDER BY next_cleanup_at, object_key LIMIT #{limit}
            """)
    List<String> objectCleanupKeys(@Param("now") Instant now, @Param("limit") int limit);

    @Update("""
            UPDATE chat_file_upload_attempt SET last_cleaned_at = #{now}, next_cleanup_at = #{nextCheck}
            WHERE object_key = #{objectKey}
            """)
    int objectCleaned(@Param("objectKey") String objectKey, @Param("now") Instant now,
            @Param("nextCheck") Instant nextCheck);

    @Update("UPDATE chat_file_upload_attempt SET next_cleanup_at = #{retryAt} WHERE object_key = #{objectKey}")
    int retryObjectCleanup(@Param("objectKey") String objectKey, @Param("retryAt") Instant retryAt);

    @Update("""
            UPDATE chat_file_upload f SET status = 'DELETED'
            WHERE status = 'DELETING' AND NOT EXISTS (
                SELECT 1 FROM chat_file_upload_attempt a WHERE a.file_id = f.file_id
                  AND (a.last_cleaned_at IS NULL OR a.last_cleaned_at < f.reclaim_started_at)
            ) LIMIT #{limit}
            """)
    int finishReclaimedFiles(@Param("limit") int limit);
}
