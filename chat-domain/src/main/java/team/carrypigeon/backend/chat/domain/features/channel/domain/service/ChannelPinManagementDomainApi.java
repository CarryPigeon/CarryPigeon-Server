package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelPinManagementApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RemoveChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.SetChannelPinCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelPinReferencesQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息置顶管理 API 实现。
 * 职责：统一执行置顶治理权限、数量上限、重复替换和 channel 置顶记录读写。
 * 边界：不读取消息模型，不发布 message 领域事件。
 */
@Service
public class ChannelPinManagementDomainApi implements ChannelPinManagementApi {

    private static final long MAX_PINS_PER_CHANNEL = 50L;

    private final ChannelMembershipService membershipService;
    private final ChannelPinRepository channelPinRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;

    public ChannelPinManagementDomainApi(
            ChannelMembershipService membershipService,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        this.membershipService = membershipService;
        this.channelPinRepository = channelPinRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
    }

    @Override
    public ChannelPinReference setPin(SetChannelPinCommand command) {
        validateSetCommand(command);
        Channel channel = membershipService.requireChannel(command.channelId());
        requireModerationPermission(channel, command.operatorAccountId());

        Optional<ChannelPin> existingPin = channelPinRepository.findByChannelIdAndMessageId(
                channel.id(), command.messageId()
        );
        if (existingPin.isEmpty() && channelPinRepository.countByChannelId(channel.id()) >= MAX_PINS_PER_CHANNEL) {
            throw ProblemException.validationFailed("pin_limit_reached", "channel pin limit is reached");
        }
        existingPin.ifPresent(pin -> channelPinRepository.delete(channel.id(), pin.messageId()));

        ChannelPin pin = new ChannelPin(
                command.pinId(),
                channel.id(),
                command.messageId(),
                command.operatorAccountId(),
                normalizeNote(command.note()),
                command.pinnedAt()
        );
        channelPinRepository.save(pin);
        return toReference(pin);
    }

    @Override
    public ChannelPinReference removePin(RemoveChannelPinCommand command) {
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        Channel channel = membershipService.requireChannel(command.channelId());
        requireModerationPermission(channel, command.operatorAccountId());
        ChannelPin pin = channelPinRepository.findByChannelIdAndMessageId(channel.id(), command.messageId())
                .orElseThrow(() -> ProblemException.notFound("channel pin does not exist"));
        channelPinRepository.delete(channel.id(), command.messageId());
        return toReference(pin);
    }

    @Override
    public List<ChannelPinReference> listPins(ListChannelPinReferencesQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 51) {
            throw ProblemException.validationFailed("limit must be between 1 and 51");
        }
        Channel channel = membershipService.requireChannel(query.channelId());
        membershipService.requireMembership(channel.id(), query.accountId());
        return channelPinRepository.findByChannelIdBefore(
                        channel.id(), query.cursorMessageId(), query.limit()
                ).stream()
                .map(this::toReference)
                .toList();
    }

    private void requireModerationPermission(Channel channel, long operatorAccountId) {
        channelGovernancePolicy.requireCanModeratePin(
                channel, membershipService.requireMembership(channel.id(), operatorAccountId)
        );
    }

    private void validateSetCommand(SetChannelPinCommand command) {
        requirePositive(command.pinId(), "pinId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        if (command.pinnedAt() == null) {
            throw ProblemException.validationFailed("pinnedAt must not be null");
        }
        if (normalizeNote(command.note()).length() > 200) {
            throw ProblemException.validationFailed("note length must be less than or equal to 200");
        }
    }

    private String normalizeNote(String note) {
        return note == null ? "" : note.trim();
    }

    private ChannelPinReference toReference(ChannelPin pin) {
        return new ChannelPinReference(
                pin.pinId(), pin.channelId(), pin.messageId(), pin.pinnedByAccountId(), pin.note(), pin.pinnedAt()
        );
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
