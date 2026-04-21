package team.carrypigeon.backend.chat.domain.features.user.domain.repository;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;

/**
 * 用户资料仓储抽象。
 * 职责：为应用层提供当前账户资料查询与更新语义入口。
 * 边界：这里只定义业务语义，不暴露数据库实现细节。
 */
public interface UserProfileRepository {

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 当前登录账户 ID
     * @return 命中时返回资料，未命中时返回空
     */
    Optional<UserProfile> findByAccountId(long accountId);

    /**
     * 保存新的用户资料。
     *
     * @param userProfile 待保存资料
     * @return 已保存资料
     */
    UserProfile save(UserProfile userProfile);

    /**
     * 更新已有用户资料。
     *
     * @param userProfile 待更新资料
     * @return 已更新资料
     */
    UserProfile update(UserProfile userProfile);
}
