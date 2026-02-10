package team.carrypigeon.backend.dao.database.mapper.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;

import java.time.LocalDateTime;

/**
 * `user` 表持久化对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("user")
public class UserPO {

    /**
     * 用户 ID。
     */
    @TableId
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 头像资源 ID。
     */
    private Long avatar;

    /**
     * 邮箱地址。
     */
    private String email;

    /**
     * 性别值。
     */
    private Integer sex;

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

    /**
     * 将 PO 转换为 BO。
     *
     * @return 用户领域对象。
     */
    public CPUser toBo() {
        return new CPUser()
                .setId(id)
                .setUsername(username)
                .setAvatar(avatar)
                .setEmail(email)
                .setSex(CPUserSexEnum.valueOf(sex))
                .setBrief(brief)
                .setBirthday(birthday)
                .setRegisterTime(registerTime);
    }

    /**
     * 从 BO 构建 PO。
     *
     * @param user 用户领域对象。
     * @return 用户持久化对象。
     */
    public static UserPO fromBo(CPUser user) {
        return new UserPO()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setAvatar(user.getAvatar())
                .setEmail(user.getEmail())
                .setSex(user.getSex().getValue())
                .setBrief(user.getBrief())
                .setBirthday(user.getBirthday())
                .setRegisterTime(user.getRegisterTime());
    }
}
