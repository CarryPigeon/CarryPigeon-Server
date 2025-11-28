package team.carrypigeon.backend.api.dao.database.user.token;

import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;

/**
 * 用户token相关dao接口
 * @author midreamsheep
 * */
public interface UserTokenDao {
    /**
     * 通过id获取token数据
     * @param id 数据id
     * */
    CPUserToken getById(long id);

    /**
     * 通过用户id获取token数据
     * @param userId 用户id
     * */
    CPUserToken[] getByUserId(long userId);

    /**
     * 通过token数据获取token数据
     * @param token token数据
     * */
    CPUserToken getByToken(String token);

    /**
     * 保存token数据（已存在则为更新，不存在则为插入）
     * @param token token数据
     * */
    boolean save(CPUserToken token);

    /**
     * 删除token数据
     * @param token token数据
     * */
    boolean delete(CPUserToken token);
}
