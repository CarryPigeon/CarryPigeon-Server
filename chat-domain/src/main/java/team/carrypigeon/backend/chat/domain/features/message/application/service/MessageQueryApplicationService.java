package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListChannelPinsQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息查询应用服务。
 * 职责：承接消息历史、搜索与置顶列表等只读查询能力。
 * 边界：这里只处理读侧校验与结果投影，不承担消息写入、广播或事务副作用。
 */
@Service
public class MessageQueryApplicationService {

    private static final String CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String FILE_MESSAGE_TYPE = "file";
    private static final String VOICE_MESSAGE_TYPE = "voice";
    private static final String TEXT_MESSAGE_TYPE = "text";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelPinRepository channelPinRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentPayloadResolver messageAttachmentPayloadResolver;

    public MessageQueryApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelPinRepository channelPinRepository,
            MessageRepository messageRepository,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelPinRepository = channelPinRepository;
        this.messageRepository = messageRepository;
        this.messageAttachmentPayloadResolver = messageAttachmentPayloadResolver;
    }

    /**
     * 查询频道置顶列表。
     *
     * @param query 置顶查询参数
     * @return 置顶结果列表
     */
    public List<ChannelPinResult> listChannelPins(ListChannelPinsQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        return channelPinRepository.findByChannelIdBefore(channel.id(), query.cursorMessageId(), query.limit() + 1).stream()
                .map(this::toPinResult)
                .toList();
    }

    /**
     * 查询频道历史消息。
     *
     * @param query 历史查询参数
     * @return 历史消息结果
     */
    public ChannelMessageHistoryResult getChannelMessageHistory(GetChannelMessageHistoryQuery query) {
        validateHistoryQuery(query);
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        if (query.aroundMessageId() != null) {
            ChannelMessage targetMessage = requireMessage(query.aroundMessageId());
            if (targetMessage.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            int beforeCount = query.before() == null ? 25 : query.before();
            int afterCount = query.after() == null ? 25 : query.after();
            List<ChannelMessageResult> messages = Stream.concat(
                            Stream.concat(
                                    messageRepository.findByChannelIdBefore(channel.id(), query.aroundMessageId(), beforeCount).stream(),
                                    Stream.of(targetMessage)
                            ),
                            messageRepository.findByChannelIdAfter(channel.id(), query.aroundMessageId(), afterCount).stream()
                    )
                    .sorted(Comparator.comparingLong(ChannelMessage::messageId).reversed())
                    .map(this::toResult)
                    .toList();
            return new ChannelMessageHistoryResult(messages, null);
        }
        List<ChannelMessageResult> messages = messageRepository.findByChannelIdBefore(
                        channel.id(),
                        query.cursorMessageId(),
                        query.limit() + 1
                ).stream()
                .map(this::toResult)
                .toList();
        boolean hasMore = messages.size() > query.limit();
        List<ChannelMessageResult> pageItems = hasMore ? messages.subList(0, query.limit()) : messages;
        Long nextCursor = hasMore ? pageItems.get(pageItems.size() - 1).messageId() : null;
        return new ChannelMessageHistoryResult(pageItems, nextCursor);
    }

    /**
     * 在频道内按关键字搜索消息。
     *
     * @param query 搜索参数
     * @return 搜索结果
     */
    public ChannelMessageSearchResult searchChannelMessages(SearchChannelMessagesQuery query) {
        validateSearchQuery(query);
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        List<ChannelMessageResult> messages = messageRepository.searchByChannelId(
                        channel.id(),
                        query.keyword().trim(),
                        query.cursorMessageId(),
                        query.senderAccountId(),
                        normalizeDomain(query.domain()),
                        query.beforeMessageId(),
                        query.afterMessageId(),
                        query.limit() + 1
                ).stream()
                .map(this::toResult)
                .toList();
        return new ChannelMessageSearchResult(messages);
    }

    private void validateHistoryQuery(GetChannelMessageHistoryQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.aroundMessageId() != null && query.aroundMessageId() <= 0) {
            throw ProblemException.validationFailed("around_mid must be greater than 0");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursorMessageId must be greater than 0");
        }
        if (query.aroundMessageId() != null && query.cursorMessageId() != null) {
            throw ProblemException.validationFailed("cursor and around_mid cannot be used together");
        }
        if (query.before() != null && (query.before() < 0 || query.before() > 50)) {
            throw ProblemException.validationFailed("before must be between 0 and 50");
        }
        if (query.after() != null && (query.after() < 0 || query.after() > 50)) {
            throw ProblemException.validationFailed("after must be between 0 and 50");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
    }

    private void validateSearchQuery(SearchChannelMessagesQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.keyword() == null || query.keyword().isBlank()) {
            throw ProblemException.validationFailed("keyword must not be blank");
        }
        if (query.keyword().trim().length() > 100) {
            throw ProblemException.validationFailed("keyword length must be between 1 and 100");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.senderAccountId() != null && query.senderAccountId() <= 0) {
            throw ProblemException.validationFailed("sender_uid must be greater than 0");
        }
        if (query.beforeMessageId() != null && query.beforeMessageId() <= 0) {
            throw ProblemException.validationFailed("before_mid must be greater than 0");
        }
        if (query.afterMessageId() != null && query.afterMessageId() <= 0) {
            throw ProblemException.validationFailed("after_mid must be greater than 0");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        return switch (domain.trim()) {
            case "Core:Text" -> TEXT_MESSAGE_TYPE;
            case "Core:File" -> FILE_MESSAGE_TYPE;
            case "Core:Voice" -> VOICE_MESSAGE_TYPE;
            default -> domain.trim();
        };
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMember requireMembership(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    private ChannelMessageResult toResult(ChannelMessage message) {
        return new ChannelMessageResult(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.body(),
                message.previewText(),
                messageAttachmentPayloadResolver.resolve(message.messageType(), message.payload()),
                message.metadata(),
                message.mentions(),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    private ChannelPinResult toPinResult(ChannelPin pin) {
        return new ChannelPinResult(pin.pinId(), pin.channelId(), pin.messageId(), pin.pinnedByAccountId(), pin.pinnedAt(), pin.note());
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
