package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthRefreshSessionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthRefreshSessionMapper;

/**
 * MyBatis-Plus 刷新会话数据库服务。
 * 职责：在 database-impl 中完成刷新会话的最小查询、写入与撤销能力。
 * 边界：只负责数据库记录映射，不承载 token 轮换业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusAuthRefreshSessionDatabaseService implements AuthRefreshSessionDatabaseService {

    private final AuthRefreshSessionMapper authRefreshSessionMapper;

    public MybatisPlusAuthRefreshSessionDatabaseService(AuthRefreshSessionMapper authRefreshSessionMapper) {
        this.authRefreshSessionMapper = authRefreshSessionMapper;
    }

    /**
     * 按会话 ID 读取刷新会话。
     */
    @Override
    public Optional<AuthRefreshSessionRecord> findById(long sessionId) {
        return Optional.ofNullable(authRefreshSessionMapper.selectById(sessionId))
                .map(AuthRefreshSessionEntity::toRecord);
    }

    /**
     * 插入新的刷新会话记录。
     */
    @Override
    public void insert(AuthRefreshSessionRecord record) {
        authRefreshSessionMapper.insert(AuthRefreshSessionEntity.from(record));
    }

    /**
     * 撤销指定刷新会话。
     */
    @Override
    public void revoke(long sessionId) {
        Instant now = Instant.now();
        authRefreshSessionMapper.update(null, new LambdaUpdateWrapper<AuthRefreshSessionEntity>()
                .eq(AuthRefreshSessionEntity::getId, sessionId)
                .set(AuthRefreshSessionEntity::getRevoked, true)
                .set(AuthRefreshSessionEntity::getUpdatedAt, now));
    }

    /**
     * 仅在会话仍可用时原子撤销。
     * 输出：抢占到撤销权时返回 true，已被其它请求撤销或已过期时返回 false。
     */
    @Override
    public boolean revokeIfActive(long sessionId) {
        Instant now = Instant.now();
        return authRefreshSessionMapper.update(null, new LambdaUpdateWrapper<AuthRefreshSessionEntity>()
                .eq(AuthRefreshSessionEntity::getId, sessionId)
                .eq(AuthRefreshSessionEntity::getRevoked, false)
                .gt(AuthRefreshSessionEntity::getExpiresAt, now)
                .set(AuthRefreshSessionEntity::getRevoked, true)
                .set(AuthRefreshSessionEntity::getUpdatedAt, now)) > 0;
    }
}
