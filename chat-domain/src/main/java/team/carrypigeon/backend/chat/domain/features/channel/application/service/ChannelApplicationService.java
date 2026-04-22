package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道应用服务。
 * 职责：编排 V0 默认频道查询用例。
 * 边界：当前阶段不承载频道创建、编辑或复杂权限管理。
 */
@Service
public class ChannelApplicationService {

    private static final String CHANNEL_NOT_FOUND_MESSAGE = "default channel does not exist";

    private final ChannelRepository channelRepository;

    public ChannelApplicationService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    /**
     * 查询当前服务端默认频道。
     *
     * @param command 默认频道查询命令
     * @return 默认频道结果
     */
    public ChannelResult getDefaultChannel(GetDefaultChannelCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        Channel channel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
        return toResult(channel);
    }

    private ChannelResult toResult(Channel channel) {
        return new ChannelResult(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.type(),
                channel.defaultChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}
