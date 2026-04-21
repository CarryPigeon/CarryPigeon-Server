package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;

/**
 * 用户资料数据库服务抽象。
 * 职责：向 chat-domain 提供用户资料查询与更新能力。
 * 边界：不暴露 SQL、JDBC 或具体数据库实现。
 */
public interface UserProfileDatabaseService {

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 当前登录账户 ID
     * @return 命中时返回用户资料记录
     */
    Optional<UserProfileRecord> findByAccountId(long accountId);

    /**
     * 写入新的用户资料。
     *
     * @param record 待保存资料记录
     */
    void insert(UserProfileRecord record);

    /**
     * 更新已有用户资料。
     *
     * @param record 待更新资料记录
     */
    void update(UserProfileRecord record);
}
