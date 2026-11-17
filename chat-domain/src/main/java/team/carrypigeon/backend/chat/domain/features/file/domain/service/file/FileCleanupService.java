package team.carrypigeon.backend.chat.domain.features.file.domain.service.file;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.domain.repository.FileUploadRepository;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 有界、可重启的文件回收。先在事务内撤下文件，再在事务外删除已登记对象。
 * 当前只有头像使用结构化文件引用；附件处理器尚未启用，不把文本中的链接猜作附件引用。
 */
@Slf4j
@Service
public class FileCleanupService {
    private static final int BATCH_SIZE = 100;
    private final FileUploadRepository uploads;
    private final ObjectProvider<ObjectStorageService> storage;
    private final TransactionRunner transactions;
    private final TimeProvider time;

    public FileCleanupService(FileUploadRepository uploads, ObjectProvider<ObjectStorageService> storage,
            TransactionRunner transactions, TimeProvider time) {
        this.uploads = uploads;
        this.storage = storage;
        this.transactions = transactions;
        this.time = time;
    }

    @Scheduled(fixedDelayString = "${cp.chat.file.cleanup-delay-ms:60000}")
    public void cleanFiles() {
        try {
            cleanNextBatch();
        } catch (RuntimeException exception) {
            log.error("File cleanup batch failed; persisted state will be retried", exception);
        }
    }

    /** 每轮最多撤下及删除各 100 项，删除失败延期重试，不阻塞其它对象。 */
    public void cleanNextBatch() {
        var now = time.nowInstant();
        for (int i = 0; i < BATCH_SIZE; i++) {
            boolean found = transactions.runInTransaction(() -> {
                Long fileId = uploads.findReclaimCandidateForUpdate(now, now.minus(Duration.ofDays(7)),
                        now.minus(Duration.ofHours(1))).orElse(null);
                if (fileId == null) {
                    return false;
                }
                uploads.beginReclaim(fileId, now);
                return true;
            });
            if (!found) {
                break;
            }
        }
        for (String objectKey : uploads.findObjectCleanupKeys(now, BATCH_SIZE)) {
            try {
                storage.getObject().delete(objectKey);
                // 保留已知对象的回收登记，按天复查，覆盖上传超时后对象存储晚完成的情况。
                // 不能把数据库与对象存储之间不存在的原子性当作保证，也不能扫描删除未知历史对象。
                uploads.objectCleaned(objectKey, time.nowInstant(), now.plus(Duration.ofDays(1)));
            } catch (RuntimeException exception) {
                uploads.retryObjectCleanup(objectKey, now.plus(Duration.ofMinutes(1)));
                log.warn("File object cleanup failed; will retry: {}", objectKey, exception);
            }
        }
        uploads.finishReclaimedFiles(BATCH_SIZE);
    }
}
