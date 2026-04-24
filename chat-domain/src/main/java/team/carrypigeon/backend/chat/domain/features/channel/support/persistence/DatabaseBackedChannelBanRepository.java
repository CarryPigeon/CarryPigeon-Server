package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;

/**
 * 基于 database-api 的频道封禁仓储适配器。
 * 职责：在 channel feature 内完成封禁领域模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
public class DatabaseBackedChannelBanRepository implements ChannelBanRepository {

    private final ChannelBanDatabaseService channelBanDatabaseService;

    public DatabaseBackedChannelBanRepository(ChannelBanDatabaseService channelBanDatabaseService) {
        this.channelBanDatabaseService = channelBanDatabaseService;
    }

    @Override
    public Optional<ChannelBan> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
        return channelBanDatabaseService.findByChannelIdAndBannedAccountId(channelId, bannedAccountId)
                .map(this::toDomain);
    }

    @Override
    public void save(ChannelBan channelBan) {
        channelBanDatabaseService.insert(toRecord(channelBan));
    }

    @Override
    public void update(ChannelBan channelBan) {
        channelBanDatabaseService.update(toRecord(channelBan));
    }

    private ChannelBan toDomain(ChannelBanRecord record) {
        return new ChannelBan(
                record.channelId(),
                record.bannedAccountId(),
                record.operatorAccountId(),
                record.reason(),
                record.expiresAt(),
                record.createdAt(),
                record.revokedAt()
        );
    }

    private ChannelBanRecord toRecord(ChannelBan channelBan) {
        return new ChannelBanRecord(
                channelBan.channelId(),
                channelBan.bannedAccountId(),
                channelBan.operatorAccountId(),
                channelBan.reason(),
                channelBan.expiresAt(),
                channelBan.createdAt(),
                channelBan.revokedAt()
        );
    }
}
