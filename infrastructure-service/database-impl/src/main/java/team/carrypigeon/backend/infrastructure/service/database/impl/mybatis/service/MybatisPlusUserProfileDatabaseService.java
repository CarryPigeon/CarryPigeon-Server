package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.UserProfileEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.UserProfileMapper;

/**
 * MyBatis-Plus 用户资料数据库服务。
 * 职责：在 database-impl 中完成用户资料的最小查询与更新。
 * 边界：只负责数据库记录映射，不承载用户资料业务规则与协议决策。
 */
public class MybatisPlusUserProfileDatabaseService implements UserProfileDatabaseService {

    private final UserProfileMapper userProfileMapper;

    public MybatisPlusUserProfileDatabaseService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public Optional<UserProfileRecord> findByAccountId(long accountId) {
        try {
            return Optional.ofNullable(userProfileMapper.selectById(accountId)).map(this::toRecord);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query user profile by account id", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query user profile by account id", exception);
        }
    }

    @Override
    public void insert(UserProfileRecord record) {
        try {
            userProfileMapper.insert(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert user profile", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert user profile", exception);
        }
    }

    @Override
    public void update(UserProfileRecord record) {
        try {
            int updatedRows = userProfileMapper.updateById(toEntity(record));
            if (updatedRows == 0) {
                throw new DatabaseServiceException("user profile update affected no rows");
            }
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to update user profile", exception);
        } catch (RuntimeException exception) {
            if (exception instanceof DatabaseServiceException databaseServiceException) {
                throw databaseServiceException;
            }
            throw new DatabaseServiceException("failed to update user profile", exception);
        }
    }

    private UserProfileRecord toRecord(UserProfileEntity entity) {
        return new UserProfileRecord(
                entity.getAccountId(),
                entity.getNickname(),
                entity.getAvatarUrl(),
                entity.getBio(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private UserProfileEntity toEntity(UserProfileRecord record) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setAccountId(record.accountId());
        entity.setNickname(record.nickname());
        entity.setAvatarUrl(record.avatarUrl());
        entity.setBio(record.bio());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }
}
