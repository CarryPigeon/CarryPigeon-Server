package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户信息请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserUpdateProfileVO {
    private String username;
    private long avatar;
    private int sex;
    private String brief;
    private long birthday;
}
