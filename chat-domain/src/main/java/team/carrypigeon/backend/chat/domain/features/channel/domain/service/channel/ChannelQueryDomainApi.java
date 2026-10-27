package team.carrypigeon.backend.chat.domain.features.channel.domain.service.channel;

import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;

/**
 * 频道发现 API 实现。
 * 职责：承接频道发现查询，把仓储返回的发现读模型转换为对外投影。
 * 边界：不判断调用方是否有权查看某个具体频道，也不承载成员、禁言与审计查询；这些由各自能力 API 暴露。
 */
@Service
public class ChannelQueryDomainApi implements ChannelQueryApi {

    /**
     * 多取一条用于判定是否还有下一页，因此实际查询上限始终比调用方请求多 1。
     */
    private static final int HAS_MORE_PROBE = 1;

    private final ChannelRepository channelRepository;

    public ChannelQueryDomainApi(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public List<DiscoverChannelResult> discoverChannels(long accountId, String keyword, Long cursorChannelId, int limit) {
        // 关键字统一在此归一：空白串与 null 等价，都表示不按关键字过滤
        String normalizedKeyword = keyword == null || keyword.isBlank()
            ? null
            : keyword.trim();
        // 多取一条交由调用方判定 hasMore，使分页语义不进入仓储层
        return channelRepository.discoverChannels(
                normalizedKeyword,
                cursorChannelId,
                limit + HAS_MORE_PROBE
            ).stream()
            .map(DiscoverChannelResult::from)
            .toList();
    }

}
