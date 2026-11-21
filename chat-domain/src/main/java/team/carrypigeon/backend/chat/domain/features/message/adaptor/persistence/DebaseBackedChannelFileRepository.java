package team.carrypigeon.backend.chat.domain.features.message.adaptor.persistence;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelFile;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelFileRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileDatabaseService;

import java.util.Optional;

@Component
public class DebaseBackedChannelFileRepository implements ChannelFileRepository {
    private final FileDatabaseService fileDatabaseService;

    public DebaseBackedChannelFileRepository(FileDatabaseService fileDatabaseService) {
        this.fileDatabaseService = fileDatabaseService;
    }

    @Override
    public ChannelFile findByShareKey(String shareKey) {
        Optional<FileRecord> file = fileDatabaseService.findByShareKey(shareKey);
        if (file.isPresent()) {
            FileRecord fileRecord = file.get();
            return new ChannelFile(
                fileRecord.fileId(),
                fileRecord.fileName(),
                fileRecord.contentType(),
                fileRecord.sha256(),
                fileRecord.sizeBytes(),
                fileRecord.completedAt()
            );
        }
        throw ProblemException.of(MessageProblem.INVALID_INPUT);
    }
}
