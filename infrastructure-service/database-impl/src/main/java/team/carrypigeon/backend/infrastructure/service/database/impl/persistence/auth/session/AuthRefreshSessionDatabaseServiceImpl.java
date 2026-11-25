package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.auth.session;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;

/**
 * MyBatis-Plus 刷新会话数据库服务。
 * 职责：在 database-impl 中完成刷新会话的最小查询、写入与撤销能力。
 * 边界：只负责数据库记录映射，不承载 token 轮换业务规则。
 */
@EnvironmentComponent(profiles = {PROD, DEV})
public class AuthRefreshSessionDatabaseServiceImpl implements AuthRefreshSessionDatabaseService {

    private final AuthRefreshSessionMapper authRefreshSessionMapper;

    private final TimeProvider timeProvider;

    public AuthRefreshSessionDatabaseServiceImpl(
            AuthRefreshSessionMapper authRefreshSessionMapper, TimeProvider timeProvider) {
        this.authRefreshSessionMapper = authRefreshSessionMapper;
        this.timeProvider = timeProvider;
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
    public boolean insert(AuthRefreshSessionRecord record) {
        int affectedRows = authRefreshSessionMapper.insert(AuthRefreshSessionEntity.from(record));
        return affectedRows == 1;
    }

    /**
     * 批量插入刷新会话记录。
     */
    @Override
    public boolean insertAll(List<AuthRefreshSessionRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = authRefreshSessionMapper.insertAll(records.stream().map(AuthRefreshSessionEntity::from).toList());
        return affectedRows == records.size();
    }

    /**
     * 按会话 ID 删除记录。
     */
    @Override
    public boolean deleteById(long sessionId) {
        int affectedRows = authRefreshSessionMapper.deleteById(sessionId);
        return affectedRows == 1;
    }

    /**
     * 按会话 ID 批量删除记录。
     */
    @Override
    public boolean deleteByIds(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return true;
        }
        int affectedRows = authRefreshSessionMapper.deleteByIds(sessionIds);
        return affectedRows > 0;
    }

    /**
     * 更新刷新会话记录。
     */
    @Override
    public boolean update(AuthRefreshSessionRecord record) {
        int affectedRows = authRefreshSessionMapper.updateById(AuthRefreshSessionEntity.from(record));
        return affectedRows == 1;
    }

    /**
     * 分页查询刷新会话记录。
     */
    @Override
    public DatabasePageResult<AuthRefreshSessionRecord> page(DatabasePageQuery query) {
        IPage<AuthRefreshSessionEntity> result = authRefreshSessionMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<AuthRefreshSessionEntity>()
                        .orderByAsc(AuthRefreshSessionEntity::getId)
        );
        return new DatabasePageResult<>(
                result.getRecords().stream().map(AuthRefreshSessionEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按会话 ID 批量查询刷新会话。
     */
    @Override
    public List<AuthRefreshSessionRecord> findByIds(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return authRefreshSessionMapper.selectByIds(sessionIds).stream()
                .map(AuthRefreshSessionEntity::toRecord)
                .toList();
    }

    /**
     * 撤销指定刷新会话。
     */
    @Override
    public void revoke(long sessionId) {
        Instant now = timeProvider.nowInstant();
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
        Instant now = timeProvider.nowInstant();
        return authRefreshSessionMapper.update(null, new LambdaUpdateWrapper<AuthRefreshSessionEntity>()
                .eq(AuthRefreshSessionEntity::getId, sessionId)
                .eq(AuthRefreshSessionEntity::getRevoked, false)
                .gt(AuthRefreshSessionEntity::getExpiresAt, now)
                .set(AuthRefreshSessionEntity::getRevoked, true)
                .set(AuthRefreshSessionEntity::getUpdatedAt, now)) > 0;
    }
}
