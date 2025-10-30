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
 * 数据库user表映射实体类
 * @author midreamsheep
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("user")
public class UserPO {
    // 用户唯一id
    @TableId
    private Long id;
    // 用户名
    private String username;
    // 用户头像的资源id
    private Long avatar;
    // 用户的邮箱
    private String email;
    // 用户的性别，0为未知，1为男性，2为女性
    private Integer sex;
    // 用户简介
    private String brief;
    // 用户的生日
    private LocalDateTime birthday;
    // 用户的注册时间
    private LocalDateTime registerTime;

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
