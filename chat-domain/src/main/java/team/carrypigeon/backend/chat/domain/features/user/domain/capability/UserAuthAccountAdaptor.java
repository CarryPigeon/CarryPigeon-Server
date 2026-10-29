package team.carrypigeon.backend.chat.domain.features.user.domain.capability;

import java.util.List;

/**
 * user feature对account feature的依赖适配器
 * */
public interface UserAuthAccountAdaptor {
    /**
     * 根据用户id查询用户account信息
     * @param userId 用户id
     * @return 用户信息
     * */
    AccountProfile findById(long userId);

    /**
     * 根据用户id列表查询用户account信息
     * @param userIds 用户id列表
     * @return 用户信息列表
     * */
    List<AccountProfile> findByIds(List<Long> userIds);
}
