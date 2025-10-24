package team.carrypigeon.backend.api.dao.database.user;

import team.carrypigeon.backend.api.bo.domain.user.CPUser;

/**
 * 用户的dao接口，用于与数据库进行交互
 * @author midreamsheep
 * */
public interface UserDao {

    /**
     * 通过id获取用户
     * @param id 用户唯一id
     * */
    CPUser getById(long id);

    /**
     * 通过邮箱获取用户
     * @param email 用户邮箱
     * */
    CPUser getByEmail(String email);

    /**
     * 更新用户数据（已存在则为更新，不存在则为插入）
     * @param user 用户数据
     * */
    boolean save(CPUser user);
}
