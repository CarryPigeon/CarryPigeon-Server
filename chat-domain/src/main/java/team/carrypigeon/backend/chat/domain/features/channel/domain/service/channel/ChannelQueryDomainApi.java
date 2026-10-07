package team.carrypigeon.backend.chat.domain.features.channel.domain.service.channel;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input.DiscoverChannelsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;

/**
 * 频道发现 API 实现。
 * 频道详情、成员、封禁和审计查询由各自能力 API 暴露。
 */
@Service
public class ChannelQueryDomainApi implements ChannelQueryApi {

    private final ChannelRepository channelRepository;

    public ChannelQueryDomainApi(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public List<DiscoverChannelResult> discoverChannels(DiscoverChannelsQuery query) {
                return channelRepository.discoverChannels(
                        query.keyword() == null || query.keyword().isBlank() ? null : query.keyword().trim(),
                        query.cursorChannelId(),
                        query.limit() + 1
                ).stream()
                .map(DiscoverChannelResult::from)
                .toList();
    }

}
