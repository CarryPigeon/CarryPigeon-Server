package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthAccountEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthAccountMapper;

/**
 * MyBatis-Plus 鉴权账户数据库服务。
 * 职责：在 database-impl 中完成鉴权账户的最小查询与写入。
 * 边界：只负责数据库记录映射，不承载注册业务规则与协议决策。
 */
public class MybatisPlusAuthAccountDatabaseService implements AuthAccountDatabaseService {

    private final AuthAccountMapper authAccountMapper;

    public MybatisPlusAuthAccountDatabaseService(AuthAccountMapper authAccountMapper) {
        this.authAccountMapper = authAccountMapper;
    }

    @Override
    public Optional<AuthAccountRecord> findByUsername(String username) {
        try {
            AuthAccountEntity entity = authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccountEntity>()
                    .eq(AuthAccountEntity::getUsername, username)
                    .last("LIMIT 1"));
            return Optional.ofNullable(entity).map(this::toRecord);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query auth account by username", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query auth account by username", exception);
        }
    }

    @Override
    public void insert(AuthAccountRecord record) {
        try {
            authAccountMapper.insert(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert auth account", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert auth account", exception);
        }
    }

    private AuthAccountRecord toRecord(AuthAccountEntity entity) {
        return new AuthAccountRecord(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AuthAccountEntity toEntity(AuthAccountRecord record) {
        AuthAccountEntity entity = new AuthAccountEntity();
        entity.setId(record.id());
        entity.setUsername(record.username());
        entity.setPasswordHash(record.passwordHash());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }
}
