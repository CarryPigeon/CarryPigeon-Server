package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户获取用户信息请求参数
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserGetProfileVO {
    private long uid;
}
