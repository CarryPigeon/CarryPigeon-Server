package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;
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
public class DatabaseBackedChannelMemberRepository implements ChannelMemberRepository {

    private final ChannelMemberDatabaseService channelMemberDatabaseService;

    public DatabaseBackedChannelMemberRepository(ChannelMemberDatabaseService channelMemberDatabaseService) {
        this.channelMemberDatabaseService = channelMemberDatabaseService;
    }

    /**
     * 判断账户是否已属于指定频道。
     * 输出：只返回成员关系是否存在，不返回角色或其它成员信息。
     */
    @Override
    public boolean exists(long channelId, long accountId) {
        return channelMemberDatabaseService.exists(channelId, accountId);
    }

    /**
     * 写入新的频道成员关系。
     * 副作用：会把成员角色、加入时间和禁言状态持久化。
     */
    @Override
    public void save(ChannelMember channelMember) {
        channelMemberDatabaseService.insert(toRecord(channelMember));
    }

    /**
     * 查询指定频道中的目标成员。
     * 输出：存在时返回领域成员模型。
     */
    @Override
    public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
        return channelMemberDatabaseService.findByChannelIdAndAccountId(channelId, accountId)
                .map(this::toDomain);
    }

    /**
     * 更新既有成员关系。
     * 边界：这里只覆盖持久化状态，不负责变更合法性校验。
     */
    @Override
    public void update(ChannelMember channelMember) {
        channelMemberDatabaseService.update(toRecord(channelMember));
    }

    /**
     * 删除成员关系。
     */
    @Override
    public void delete(long channelId, long accountId) {
        channelMemberDatabaseService.delete(channelId, accountId);
    }

    /**
     * 列出频道当前所有成员。
     */
    @Override
    public List<ChannelMember> findByChannelId(long channelId) {
        return channelMemberDatabaseService.findByChannelId(channelId).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 返回频道内所有成员账户 ID。
     * 原因：供 realtime 广播和治理决策快速定位接收者集合。
     */
    @Override
    public List<Long> findAccountIdsByChannelId(long channelId) {
        return channelMemberDatabaseService.findAccountIdsByChannelId(channelId);
    }

    /**
     * 返回账户当前加入的所有频道 ID。
     */
    @Override
    public List<Long> findChannelIdsByAccountId(long accountId) {
        return channelMemberDatabaseService.findChannelIdsByAccountId(accountId);
    }

    private ChannelMember toDomain(ChannelMemberRecord record) {
        return new ChannelMember(
                record.channelId(),
                record.accountId(),
                ChannelMemberRole.valueOf(record.role()),
                record.joinedAt(),
                record.mutedUntil()
        );
    }

    private ChannelMemberRecord toRecord(ChannelMember channelMember) {
        return new ChannelMemberRecord(
                channelMember.channelId(),
                channelMember.accountId(),
                channelMember.role().name(),
                channelMember.joinedAt(),
                channelMember.mutedUntil()
        );
    }
}
