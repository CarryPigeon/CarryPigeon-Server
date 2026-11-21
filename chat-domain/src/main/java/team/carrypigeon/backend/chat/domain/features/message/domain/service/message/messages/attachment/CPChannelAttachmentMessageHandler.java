package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.attachment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelFile;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelFileRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.annotation.MessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageHandler;

/**
 * 附件消息处理器
 */
@MessageHandler(CPChannelAttachmentMessageHandler.DOMAIN)
public class CPChannelAttachmentMessageHandler implements CPChannelMessageHandler {
    public final static String DOMAIN = "Core:Attachment:V1";
    private final ObjectMapper objectMapper;
    private final ChannelFileRepository channelFileRepository;

    public CPChannelAttachmentMessageHandler(ObjectMapper objectMapper, ChannelFileRepository channelFileRepository) {
        this.objectMapper = objectMapper;
        this.channelFileRepository = channelFileRepository;
    }

    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        CPChannelAttachmentOriginalData originData = objectMapper.convertValue(message.data(), CPChannelAttachmentOriginalData.class);
        ChannelFile channelFile = channelFileRepository.findByShareKey(originData.getShareKey());
        CPChannelAttachmentData data = new CPChannelAttachmentData(
            originData.getShareKey(),
            channelFile.fileName(),
            channelFile.size(),
            channelFile.fileType(),
            channelFile.sha256(),
            channelFile.createdAt()
        );
        return new CPChannelMessage(
            message.messageId(),
            message.senderId(),
            message.channelId(),
            message.domain(),
            objectMapper.convertValue(data, new TypeReference<>() {}),
            message.sendTime(),
            message.mentions(),
            data.getFileName(),
            data.getFileName(),
            message.status()
        );
    }
}
