package team.carrypigeon.backend.api.dao.database.user;

import team.carrypigeon.backend.api.bo.domain.user.CPUser;

/**
 * 用户 DAO 接口。
 * <p>
 * 该接口属于跨模块稳定契约层，业务逻辑应依赖本接口而非具体实现。
 */
public interface UserDao {

    /**
     * 按用户 ID 查询。
     *
     * @param id 用户 ID
     * @return 用户实体，不存在时返回 null
     */
    CPUser getById(long id);

    /**
     * 按邮箱查询。
     *
     * @param email 邮箱
     * @return 用户实体，不存在时返回 null
     */
    CPUser getByEmail(String email);

    /**
     * 保存用户（新增或更新）。
     *
     * @param user 用户实体
     * @return 保存是否成功
     */
    boolean save(CPUser user);

    /**
     * 按 ID 集合批量查询用户。
     *
     * @param ids 用户 ID 集合
     * @return 命中的用户列表
     */
    java.util.List<CPUser> listByIds(java.util.Collection<Long> ids);
}
