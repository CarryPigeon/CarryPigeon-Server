package team.carrypigeon.backend.chat.domain.features.message.domain.service.mention;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.MentionApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result.MentionPage;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result.MentionResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 提及领域服务。
 * 职责：承接提及列表查询与已读状态更新用例。
 * 边界：不承载请求字段校验；这里只编排提及读取和已读变更，不负责提及生成。
 */
@Service
public class MentionService implements MentionApi {

    private final MentionRepository mentionRepository;
    private final ChannelContextApi channelContextApi;

    public MentionService(MentionRepository mentionRepository, ChannelContextApi channelContextApi) {
        this.mentionRepository = mentionRepository;
        this.channelContextApi = channelContextApi;
    }

    @Override
    public MentionResult getMention(long accountId, long mentionId) {
        return mentionRepository.findVisibleByIds(accountId, List.of(mentionId)).stream()
                .findFirst().map(this::toResult)
                .orElseThrow(() -> ProblemException.of(MessageProblem.MENTION_NOT_FOUND));
    }

    @Override
    public List<MentionResult> getMentions(long accountId, List<Long> mentionIds) {
        List<Long> ids = mentionIds.stream().distinct().toList();
        // SQL IN 没有输入顺序保证，按请求还原并略过并发撤回的提及。
        Map<Long, Mention> visible = mentionRepository.findVisibleByIds(accountId, ids).stream()
                .collect(Collectors.toMap(Mention::mentionId, Function.identity()));
        return ids.stream().map(visible::get).filter(Objects::nonNull).map(this::toResult).toList();
    }

    @Override
    public MentionPage listMentions(ListMentionsQuery query) {
        return readPage(query, null);
    }

    @Override
    public MentionPage listChannelMentions(long channelId, ListMentionsQuery query) {
        channelContextApi.requireMemberContext(channelId, query.accountId());
        return readPage(query, channelId);
    }

    private MentionPage readPage(ListMentionsQuery query, Long channelId) {
        List<Mention> rows = mentionRepository.listByAccountId(
                query.accountId(), query.cursorMentionId(), query.limit() + 1, query.unreadOnly(), channelId);
        List<MentionResult> items = rows.stream().limit(query.limit()).map(this::toResult).toList();
        Long next = rows.size() > query.limit() ? items.getLast().mentionId() : null;
        return new MentionPage(items, next);
    }

    @Override
    public void markMentionRead(long accountId, long mentionId) {
        if (!mentionRepository.markAsRead(accountId, mentionId)) {
            throw ProblemException.of(MessageProblem.MENTION_NOT_FOUND);
        }
    }

    @Override
    public void markMentionsRead(long accountId, Long throughMentionId) {
        mentionRepository.markAllAsRead(accountId, throughMentionId, null);
    }

    @Override
    public void markChannelMentionsRead(long accountId, long channelId, Long throughMentionId) {
        channelContextApi.requireMemberContext(channelId, accountId);
        mentionRepository.markAllAsRead(accountId, throughMentionId, channelId);
    }

    private MentionResult toResult(Mention mention) {
        return new MentionResult(
            mention.mentionId(), mention.channelId(), mention.messageId(), mention.fromAccountId(),
            mention.targetType(), mention.targetAccountId(), mention.createdAt(), mention.read()
        );
    }
}
