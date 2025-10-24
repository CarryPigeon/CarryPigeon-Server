package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 获取用户信息结果
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPUserGetProfileResult {
    private String username;
    private long avatar;
    private String email;
    private int sex;
    private String brief;
    private long birthday;
}
