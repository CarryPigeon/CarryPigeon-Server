package team.carrypigeon.backend.api.bo.domain.user;

import java.time.LocalDateTime;

/**
 * CarryPigeon用户实体类，主要在chat-domain模块中作为用户信息
 * */
public class CPUser {
    // 用户唯一id
    private long id;
    // 用户名
    private String username;
    // 头像id
    private long avatar;
    // 用户邮箱
    private String email;
    // 用户的性别，0为未知，1为男性，2为女性
    private CPUserSexEnum sex;
    // 用户简介
    private String brief;
    // 用户的生日
    private LocalDateTime birthday;
    // 用户的注册时间
    private LocalDateTime registerTime;
}
