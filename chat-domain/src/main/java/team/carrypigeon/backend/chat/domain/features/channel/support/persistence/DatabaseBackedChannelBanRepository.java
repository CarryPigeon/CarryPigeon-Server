package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;

/**
 * 基于 database-api 的频道禁言仓储适配器。
 * 职责：把 {@link ChannelBanRepository} 的领域语义调用翻译为 database-api 的最小数据库契约，并完成禁言记录的双向转换。
 * 边界：不含 SQL 与数据库驱动细节，不承载"禁言是否仍然生效"的判定规则；事务边界由调用方（领域服务）决定。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackedChannelBanRepository implements ChannelBanRepository {

    private final ChannelBanDatabaseService channelBanDatabaseService;

    public DatabaseBackedChannelBanRepository(ChannelBanDatabaseService channelBanDatabaseService) {
        this.channelBanDatabaseService = channelBanDatabaseService;
    }

    @Override
    public Optional<ChannelBan> findByRelation(long channelId, long bannedAccountId) {
        return channelBanDatabaseService.findByChannelIdAndBannedAccountId(channelId, bannedAccountId)
                .map(this::toDomain);
    }

    @Override
    public List<ChannelBan> list(long channelId) {
        // 返回该频道全部禁言记录（含已解除与已过期），是否仍然生效由调用方按 revokedAt 与 expiresAt 判定
        return channelBanDatabaseService.findByChannelId(channelId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(ChannelBan channelBan) {
        channelBanDatabaseService.insert(toRecord(channelBan));
    }

    @Override
    public void update(ChannelBan channelBan) {
        // 底层按 banId 更新，调用方必须沿用既有 banId，重新发号会命中不到任何行
        channelBanDatabaseService.update(toRecord(channelBan));
    }

    /**
     * 把禁言记录持久化契约转换为领域模型。
     * 说明：两侧字段一一对应；expiresAt 为空表示不设置自动到期，revokedAt 为空表示尚未解除，两者共同决定记录是否生效。
     *
     * @param record 频道禁言持久化投影
     * @return 频道禁言领域模型
     */
    private ChannelBan toDomain(ChannelBanRecord record) {
        return new ChannelBan(
                record.banId(),
                record.channelId(),
                record.bannedAccountId(),
                record.operatorAccountId(),
                record.reason(),
                record.expiresAt(),
                record.createdAt(),
                record.revokedAt()
        );
    }

    /**
     * 把禁言记录领域模型转换为持久化契约。
     * 说明：更新既有记录时必须保留原 banId，否则会被当成新记录插入或更新不到目标行。
     *
     * @param channelBan 频道禁言领域模型
     * @return 频道禁言持久化投影
     */
    private ChannelBanRecord toRecord(ChannelBan channelBan) {
        return new ChannelBanRecord(
                channelBan.banId(),
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
