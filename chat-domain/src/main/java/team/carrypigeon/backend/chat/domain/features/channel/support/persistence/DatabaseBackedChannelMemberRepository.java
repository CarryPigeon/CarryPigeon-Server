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
 * 职责：把 {@link ChannelMemberRepository} 的领域语义调用翻译为 database-api 的最小数据库契约，并完成成员关系模型的双向转换。
 * 边界：不含 SQL 与数据库驱动细节，不承载成员角色规则与治理权限判断；事务边界由调用方（领域服务）决定。
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
        // 底层按成员主键更新，调用方必须传入保留原主键的成员模型，否则不会命中任何行
        channelMemberDatabaseService.update(toRecord(channelMember));
    }

    @Override
    public void delete(long id) {
        // 这里按成员关系主键删除，不是按账号删除；移除单个成员请传 ChannelMember.id
        channelMemberDatabaseService.delete(id);
    }

    @Override
    public void deleteByChannelId(long channelId) {
        // 集合级删除，用于频道删除等需要一次性清空频道成员关系的场景
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

    /**
     * 把成员关系持久化契约转换为领域模型。
     * 说明：两侧对主键的命名不同——持久化侧为 memberId，领域侧为 id，其余字段一一对应；role 由字符串还原为固定枚举，非法取值会抛 IllegalArgumentException。
     *
     * @param record 频道成员持久化投影
     * @return 频道成员领域模型
     */
    private ChannelMember toDomain(ChannelMemberRecord record) {
        return new ChannelMember(
            record.memberId(),
            record.channelId(),
            record.accountId(),
            ChannelMemberRole.valueOf(record.role()),
            record.joinedAt()
        );
    }

    /**
     * 把成员关系领域模型转换为持久化契约。
     * 说明：领域侧的 id 会写入持久化侧的 memberId；role 以枚举名落库，因此枚举常量改名等同于数据迁移。
     *
     * @param channelMember 频道成员领域模型
     * @return 频道成员持久化投影
     */
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
