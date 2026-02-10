package team.carrypigeon.backend.api.bo.domain.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户领域对象。
 * <p>
 * 承载用户资料基础信息，不包含会话态与权限态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPUser {

    /**
     * 用户 ID。
     */
    private long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 头像资源 ID。
     */
    private long avatar;

    /**
     * 邮箱地址。
     */
    private String email;

    /**
     * 性别。
     */
    private CPUserSexEnum sex;

    /**
     * 个人简介。
     */
    private String brief;

    /**
     * 出生时间。
     */
    private LocalDateTime birthday;

    /**
     * 注册时间。
     */
    private LocalDateTime registerTime;
}
