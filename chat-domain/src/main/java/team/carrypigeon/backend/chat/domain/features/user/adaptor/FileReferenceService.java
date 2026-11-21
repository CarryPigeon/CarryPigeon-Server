package team.carrypigeon.backend.chat.domain.features.user.adaptor;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.user.domain.capability.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileDatabaseService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** 当前业务中的头像文件引用；不从任意 JSON 字段或纯文本内容猜测文件归属。 */
@Service
public class FileReferenceService implements FileReferenceApi {
    private final FileDatabaseService fileRepository;
    private final UserProfileRepository userProfileRepository;


    public FileReferenceService(FileDatabaseService fileRepository, UserProfileRepository userProfileRepository) {
        this.fileRepository = fileRepository;
        this.userProfileRepository = userProfileRepository;
    }


    @Override
    public void replaceAvatarReference(String originalShareKey, String newShareKey) {
        Optional<FileRecord> original = fileRepository.findByShareKey(originalShareKey);
        Optional<FileRecord> latest = fileRepository.findByShareKey(newShareKey);
        latest.ifPresent(latestFile -> fileRepository.update(
            new FileRecord(
                latestFile.fileId(),
                latestFile.ownerAccountId(),
                latestFile.fileName(),
                latestFile.shareKey(),
                latestFile.sizeBytes(),
                latestFile.status(),
                latestFile.objectKey(),
                latestFile.sha256(),
                latestFile.contentType(),
                latestFile.expiresAt().plus(Duration.ofDays(2650)),
                latestFile.completedAt()
            )
        ));
        original.ifPresent(originalFile -> {
                // 查询是否还有其他引用
                if (userProfileRepository.containShareKeyReference(originalFile.shareKey())) {
                    return;
                }
                fileRepository.update(
                    new FileRecord(
                        originalFile.fileId(),
                        originalFile.ownerAccountId(),
                        originalFile.fileName(),
                        originalFile.shareKey(),
                        originalFile.sizeBytes(),
                        originalFile.status(),
                        originalFile.objectKey(),
                        originalFile.sha256(),
                        originalFile.contentType(),
                        Instant.now().plus(Duration.ofDays(7)),
                        originalFile.completedAt()
                    )
                );
            }
        );
    }
}
