package team.carrypigeon.backend.api.dao.database.user.token;

import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;

/**
 * 用户令牌 DAO 接口。
 * <p>
 * 提供用户刷新令牌实体的查询、保存与删除能力。
 */
public interface UserTokenDao {

    /**
     * 按令牌记录 ID 查询。
     *
     * @param id 令牌记录 ID。
     * @return 令牌实体；不存在时返回 {@code null}。
     */
    CPUserToken getById(long id);

    /**
     * 按用户 ID 查询全部令牌。
     *
     * @param userId 用户 ID。
     * @return 令牌数组。
     */
    CPUserToken[] getByUserId(long userId);

    /**
     * 按令牌字符串查询。
     *
     * @param token 令牌字符串。
     * @return 令牌实体；不存在时返回 {@code null}。
     */
    CPUserToken getByToken(String token);

    /**
     * 保存令牌实体（新增或更新）。
     *
     * @param token 令牌实体。
     * @return 保存成功返回 {@code true}，否则返回 {@code false}。
     */
    boolean save(CPUserToken token);

    /**
     * 删除令牌实体。
     *
     * @param token 令牌实体。
     * @return 删除成功返回 {@code true}，否则返回 {@code false}。
     */
    boolean delete(CPUserToken token);
}
