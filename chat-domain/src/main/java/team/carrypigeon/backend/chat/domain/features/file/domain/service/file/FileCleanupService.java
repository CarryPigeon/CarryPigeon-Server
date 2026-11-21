package team.carrypigeon.backend.chat.domain.features.file.domain.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.domain.repository.FileRepository;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 有界、可重启的文件回收。先在事务内撤下文件，再在事务外删除已登记对象。
 * 当前只有头像使用结构化文件引用；附件处理器尚未启用，不把文本中的链接猜作附件引用。
 */
@Slf4j
@Service
public class FileCleanupService {
    private final FileRepository repository;
    private final ObjectStorageService storage;
    public FileCleanupService(FileRepository repository, ObjectStorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Scheduled(fixedDelayString = "${cp.chat.file.cleanup-delay-ms:86400000}")
    public void cleanFiles() {
        try {
            repository.findExpired().forEach(file -> {
                // 清理文件
                storage.delete(file.objectKey());
                // 清理数据库记录
                repository.delete(file.fileId());
            });
        } catch (RuntimeException exception) {
            log.error("File cleanup batch failed; persisted state will be retried", exception);
        }
    }

}
