package team.carrypigeon.backend.chat.domain.features.user.domain.repository;

import java.util.List;
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
     * 查询全部用户资料。
     *
     * @return 用户资料列表
     */
    List<UserProfile> findAll();

    /**
     * 按账户 ID 集合查询用户资料。
     *
     * @param accountIds 目标账户 ID 集合
     * @return 命中的用户资料列表
     */
    default List<UserProfile> findByAccountIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return findAll().stream()
                .filter(userProfile -> accountIds.contains(userProfile.accountId()))
                .toList();
    }

    /**
     * 按账户 ID 游标查询用户资料分页。
     *
     * @param cursorAccountId 游标账户 ID，可为空
     * @param limit 查询条数
     * @return 用户资料列表
     */
    List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit);

    /**
     * 按关键字搜索用户资料。
     *
     * @param keyword 搜索关键字
     * @param cursorAccountId 游标账户 ID，可为空
     * @param limit 查询条数
     * @return 命中用户资料列表
     */
    List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit);

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
