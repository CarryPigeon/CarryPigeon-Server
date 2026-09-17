package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * MessageAttachmentUploader 单元测试。
 * 职责：验证消息文件和语音附件在写入对象存储前执行各自大小边界。
 * 边界：不校验频道权限，也不访问真实对象存储。
 */
@Tag("unit")
class MessageAttachmentUploaderTests {

    /**
     * 验证普通文件超过 100 MiB 时被领域边界拒绝。
     */
    @Test
    @DisplayName("upload oversized file throws validation problem")
    void upload_oversizedFile_throwsValidationProblem() {
        MessageAttachmentUploader uploader = uploader();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> uploader.upload(
                        1001L,
                        1L,
                        "file",
                        "large.bin",
                        "application/octet-stream",
                        100L * 1024 * 1024 + 1,
                        new ByteArrayInputStream(new byte[] {1})
                )
        );

        assertEquals("size must be less than or equal to 104857600", exception.getMessage());
    }

    /**
     * 验证语音附件超过 20 MiB 时被更严格的领域边界拒绝。
     */
    @Test
    @DisplayName("upload oversized voice throws validation problem")
    void upload_oversizedVoice_throwsValidationProblem() {
        MessageAttachmentUploader uploader = uploader();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> uploader.upload(
                        1001L,
                        1L,
                        "voice",
                        "voice.mp3",
                        "audio/mpeg",
                        20L * 1024 * 1024 + 1,
                        new ByteArrayInputStream(new byte[] {1})
                )
        );

        assertEquals("size must be less than or equal to 20971520", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private MessageAttachmentUploader uploader() {
        return new MessageAttachmentUploader(
                mock(FileReferenceApi.class),
                mock(IdGenerator.class),
                mock(ObjectProvider.class)
        );
    }
}
