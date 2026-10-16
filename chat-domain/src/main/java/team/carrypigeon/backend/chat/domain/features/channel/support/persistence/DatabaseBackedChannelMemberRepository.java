package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;

/**
 * 基于 database-api 的频道成员仓储适配器。
 * 职责：在 channel feature 内完成成员关系模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedChannelMemberRepository implements ChannelMemberRepository {

    private final ChannelMemberDatabaseService channelMemberDatabaseService;

    public DatabaseBackedChannelMemberRepository(ChannelMemberDatabaseService channelMemberDatabaseService) {
        this.channelMemberDatabaseService = channelMemberDatabaseService;
    }


    @Override
    public void save(ChannelMember channelMember) {
        channelMemberDatabaseService.insert(toRecord(channelMember));
    }

    @Override
    public Optional<ChannelMember> findByRelation(long channelId, long accountId) {
        return channelMemberDatabaseService.findByRelation(channelId, accountId).map(this::toDomain);
    }

    @Override
    public void update(ChannelMember channelMember) {
        channelMemberDatabaseService.update(toRecord(channelMember));
    }

    @Override
    public void delete(long id) {
        channelMemberDatabaseService.delete(id);
    }

    @Override
    public void deleteByChannelId(long channelId) {
        channelMemberDatabaseService.deleteByChannelId(channelId);
    }

    @Override
    public List<ChannelMember> listByChannelId(long channelId) {
        return channelMemberDatabaseService.findMembersByChannelId(channelId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ChannelMember> listByAccountId(long accountId) {
        return channelMemberDatabaseService.findMembersByAccountId(accountId).stream().map(this::toDomain).toList();
    }

    private ChannelMember toDomain(ChannelMemberRecord record) {
        return new ChannelMember(
            record.memberId(),
            record.channelId(),
            record.accountId(),
            ChannelMemberRole.valueOf(record.role()),
            record.joinedAt()
        );
    }

    private ChannelMemberRecord toRecord(ChannelMember channelMember) {
        return new ChannelMemberRecord(
            channelMember.id(),
            channelMember.channelId(),
            channelMember.accountId(),
            channelMember.role().name(),
            channelMember.joinedAt()
        );
    }
}
