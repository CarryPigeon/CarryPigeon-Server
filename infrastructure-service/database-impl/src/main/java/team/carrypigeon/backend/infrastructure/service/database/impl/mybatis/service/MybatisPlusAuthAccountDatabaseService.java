package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthAccountEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthAccountMapper;

/**
 * MyBatis-Plus 鉴权账户数据库服务。
 * 职责：在 database-impl 中完成鉴权账户的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载注册与登录业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusAuthAccountDatabaseService implements AuthAccountDatabaseService {

    private final AuthAccountMapper authAccountMapper;

    public MybatisPlusAuthAccountDatabaseService(AuthAccountMapper authAccountMapper) {
        this.authAccountMapper = authAccountMapper;
    }

    /**
     * 按用户名查询鉴权账户。
     */
    @Override
    public Optional<AuthAccountRecord> findByUsername(String username) {
        return Optional.ofNullable(authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccountEntity>()
                        .eq(AuthAccountEntity::getUsername, username)))
                .map(AuthAccountEntity::toRecord);
    }

    /**
     * 按邮箱查询鉴权账户。
     */
    @Override
    public Optional<AuthAccountRecord> findByEmail(String email) {
        return Optional.ofNullable(authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccountEntity>()
                        .eq(AuthAccountEntity::getEmail, email)))
                .map(AuthAccountEntity::toRecord);
    }

    /**
     * 按账户 ID 查询鉴权账户。
     */
    @Override
    public Optional<AuthAccountRecord> findById(long accountId) {
        return Optional.ofNullable(authAccountMapper.selectById(accountId))
                .map(AuthAccountEntity::toRecord);
    }

    /**
     * 批量读取鉴权账户。
     */
    @Override
    public List<AuthAccountRecord> findByIds(Collection<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return authAccountMapper.selectByIds(accountIds).stream()
                .map(AuthAccountEntity::toRecord)
                .toList();
    }

    /**
     * 插入新的鉴权账户记录。
     */
    @Override
    public void insert(AuthAccountRecord record) {
        authAccountMapper.insert(AuthAccountEntity.from(record));
    }

    /**
     * 更新既有鉴权账户记录。
     */
    @Override
    public void update(AuthAccountRecord record) {
        authAccountMapper.updateById(AuthAccountEntity.from(record));
    }
}
