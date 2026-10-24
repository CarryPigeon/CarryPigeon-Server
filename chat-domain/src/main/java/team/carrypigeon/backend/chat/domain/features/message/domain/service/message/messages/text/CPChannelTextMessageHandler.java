package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.result.UserProfileResult;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.MessageHandler;

/**
 * CarryPigeon 预定义的文本消息处理器<br/>
 * 职责：处理文本消息，校验其格式正确<br/>
 * 边界：不关心消息的生命周期，只关注消息的格式<br/>
 *
 * 预处理消息格式:{@link CPChannelTextMessageOriginData}<br/>
 * 入库格式：{@link CPChannelTextMessageData}
 * */
@MessageHandler("Core:Text:V1")
public class CPChannelTextMessageHandler implements CPChannelMessageHandler {
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    private final UserProfileApi userProfileApi;

    public CPChannelTextMessageHandler(
        ObjectMapper objectMapper,
        MessageRepository messageRepository, UserProfileApi userProfileApi
    ) {
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
        this.userProfileApi = userProfileApi;
    }

    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        // 获取消息
        CPChannelTextMessageOriginData originData = objectMapper.convertValue(message.data(), CPChannelTextMessageOriginData.class);
        CPChannelTextMessageData data = new CPChannelTextMessageData();
        // 对消息进行校验
        if (originData == null) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT);
        }
        // 数据输入
        if(!originData.getReplyToMid().isBlank()){
            // 查询
            CPChannelMessage cpChannelMessage = messageRepository.findById(Long.parseLong(originData.getReplyToMid()))
                .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            // 查询发送人
            UserProfileResult user = userProfileApi.findById(cpChannelMessage.senderId());
            // 组装数据
            data.setReplyToUser(
                new CPChannelTextMessageData.CPChannelTextMessageReplyToUser(
                    user.username(),
                    cpChannelMessage.preview(),
                     cpChannelMessage.sendTime().toEpochMilli()
                    )
            );
            data.setReplyToMid(originData.getReplyToMid());
        }
        return new CPChannelMessage(
            message.messageId(),
            message.senderId(),
            message.channelId(),
            message.domain(),
            objectMapper.convertValue(data, new TypeReference<>() {}),
            message.sendTime(),
            message.mentions(),
            originData.getContent().length()>=20?originData.getContent().substring(0, 20):originData.getContent(),
            message.status()
        );
    }
}
