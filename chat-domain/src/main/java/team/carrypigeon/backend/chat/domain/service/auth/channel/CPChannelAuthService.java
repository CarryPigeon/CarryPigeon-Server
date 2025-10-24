package team.carrypigeon.backend.chat.domain.service.auth.channel;

import org.springframework.stereotype.Service;

/**
 * 通道相关的权限校验服务
 * @author midreamsheep
 * */
@Service
public class CPChannelAuthService {
    /**
     * 获取用户对通道的权限
     * @param uid 用户id
     * @param cid 通道id
     * @return 权限值 -1是非成员，0为普通成员，1为管理员，2为已被踢出
     * */
    public int getChannelAuth(long uid, long cid) {
        //TODO 获取用户权限
        return 0;
    }


}