package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.io.InputStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageAttachmentApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 频道消息附件领域 API 实现。
 * 职责：直接承载消息附件上传用例实现。
 * 边界：不负责普通文件传输和频道消息发送。
 */
@Service
public class ChannelMessageAttachmentDomainApi implements ChannelMessageAttachmentApi {

    private final MessageChannelBoundary messageChannelBoundary;
    private final MessageAttachmentUploader messageAttachmentUploader;
    private final TimeProvider timeProvider;

    public ChannelMessageAttachmentDomainApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.messageChannelBoundary = messageChannelBoundary;
        this.messageAttachmentUploader = new MessageAttachmentUploader(
                messageAttachmentObjectKeyPolicy,
                idGenerator,
                objectStorageServiceProvider
        );
        this.timeProvider = timeProvider;
    }

    @Override
    public MessageAttachmentUploadResult uploadMessageAttachment(
            long accountId,
            long channelId,
            String messageType,
            String filename,
            String mimeType,
            long size,
            InputStream content
    ) {
        requirePositive(accountId, "accountId");
        requirePositive(channelId, "channelId");
        MessageChannelBoundary.MessageChannel channel = messageChannelBoundary.requireSendableChannel(
                channelId,
                accountId,
                timeProvider.nowInstant()
        );
        return messageAttachmentUploader.upload(accountId, channel.id(), messageType, filename, mimeType, size, content);
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
