package team.carrypigeon.backend.chat.domain.features.user.domain.api.profile;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.input.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.result.UserProfileResult;

/**
 * 用户资料领域 API。
 * 职责：暴露当前用户资料、公开资料查询和资料更新能力。
 */
public interface UserProfileApi {


    /**
     * 按账号 ID 查询用户资料。
     *
     * @param accountId 用户id
     * @return 目标账号的用户资料投影
     */
    UserProfileResult findById(long accountId);

    /**
     * 批量查询公开用户资料。
     *
     * @param accountIds 目标账号 ID 列表
     * @return 公开用户资料投影列表
     */
    List<UserProfileResult> getPublicUserProfiles(List<Long> accountIds);

    /**
     * 更新当前账号用户资料。
     *
     * @param command 当前用户资料更新业务命令
     */
    void updateUserProfile(UpdateCurrentUserProfileCommand command);

}
