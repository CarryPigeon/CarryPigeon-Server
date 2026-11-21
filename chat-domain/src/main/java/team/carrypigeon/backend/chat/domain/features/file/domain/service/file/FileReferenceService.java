package team.carrypigeon.backend.chat.domain.features.file.domain.service.file;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.reference.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.model.CPFile;
import team.carrypigeon.backend.chat.domain.features.file.domain.problem.FileProblem;
import team.carrypigeon.backend.chat.domain.features.file.domain.repository.FileRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/** 当前业务中的头像文件引用；不从任意 JSON 字段或纯文本内容猜测文件归属。 */
@Service
public class FileReferenceService implements FileReferenceApi {
    private static final String DOWNLOAD_PATH = "/api/files/download/";
    private final FileRepository uploads;
    private final TimeProvider time;

    public FileReferenceService(FileRepository uploads, TimeProvider time) {
        this.uploads = uploads;
        this.time = time;
    }

    @Override
    public void replaceAvatarReference(long accountId, String avatarUrl) {
        Long previousId = uploads.avatarFileId(accountId).orElse(null);
        CPFile target = referencedUpload(avatarUrl);
        Long targetId = target == null ? null : target.fileId();
        // 不同账号可能交换头像；按文件 ID 排序持锁，避免相反顺序锁定两个文件。
        for (Long fileId : Stream.of(previousId, targetId).filter(Objects::nonNull).distinct().sorted().toList()) {
            CPFile locked = uploads.findByIdForUpdate(fileId)
                    .orElseThrow(() -> ProblemException.of(FileProblem.FILE_NOT_FOUND));
            if (fileId.equals(targetId) && locked.status() != CPFile.Status.COMPLETED) {
                throw ProblemException.of(FileProblem.FILE_NOT_FOUND);
            }
        }
        if (Objects.equals(previousId, targetId)) {
            return;
        }
        uploads.replaceAvatarReference(accountId, targetId);
        if (previousId != null) {
            uploads.reclaimIfUnreferenced(previousId, time.nowInstant());
        }
    }

    private CPFile referencedUpload(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(avatarUrl);
        } catch (IllegalArgumentException exception) {
            return null; // 外部头像地址的格式规则仍由用户资料入口负责。
        }
        String path = uri.getPath();
        if (path == null || !path.startsWith(DOWNLOAD_PATH)) {
            return null;
        }
        String shareKey = path.substring(DOWNLOAD_PATH.length());
        // server/head 等系统资源不是通用上传，不进入文件元数据与回收流程。
        if (!shareKey.startsWith("shr_")) {
            return null;
        }
        CPFile upload = uploads.findByShareKey(shareKey).orElse(null);
        if (upload == null && !uri.isAbsolute()) {
            throw ProblemException.of(FileProblem.FILE_NOT_FOUND);
        }
        // 绝对 URL 也可能来自外部服务；只有能匹配本服务元数据时才登记，宁可保留也不误删。
        return upload;
    }
}
