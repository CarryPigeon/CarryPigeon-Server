package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageAttachmentApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UploadMessageAttachmentCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 频道消息附件领域 API 实现。
 * 职责：直接承载消息附件上传用例实现。
 * 边界：不负责普通文件传输和频道消息发送。
 */
@Service
public class ChannelMessageAttachmentDomainApi implements ChannelMessageAttachmentApi {

    private final ChannelContextApi channelContextApi;
    private final MessageAttachmentUploader messageAttachmentUploader;

    public ChannelMessageAttachmentDomainApi(
            ChannelContextApi channelContextApi,
            FileReferenceApi fileReferenceApi,
            IdGenerator idGenerator,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.channelContextApi = channelContextApi;
        this.messageAttachmentUploader = new MessageAttachmentUploader(
                fileReferenceApi,
                idGenerator,
                objectStorageServiceProvider
        );
    }

    @Override
    public MessageAttachmentUploadResult uploadMessageAttachment(UploadMessageAttachmentCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        ChannelContext channel = channelContextApi.requireMemberContext(
                command.channelId(),
                command.accountId()
        );
        if (channel.isMuted()) {
            throw ProblemException.forbidden("user_muted", "channel member is muted");
        }
        return messageAttachmentUploader.upload(
                command.accountId(),
                channel.channelId(),
                command.messageType(),
                command.filename(),
                command.mimeType(),
                command.size(),
                command.content()
        );
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
