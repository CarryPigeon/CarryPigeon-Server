package team.carrypigeon.backend.api.dao.user;

import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;

/**
 * 关于用户相关的DAO接口,用于保存用户相关的信息
 * */
public interface CPUserDAO {
    /**
     * 通过用户id获取用户对象
     * */
    CPUserBO getById(long id);

    /**
     * 通过用户id删除用户
     * */
    boolean removeById(long id);

    /**
     * 向数据库添加一个用户
     * */
    boolean register(CPUserBO user, String password);
    /**
     * 更新数据库用户数据
     * 数据库数据更新时需通知所有好友对象更新相关数据
     * */
    boolean update(CPUserBO user, String password);

    /**
     * 通过邮箱和密码登录并返回用户对象
     * */
    CPUserBO login(String email, String password);
}