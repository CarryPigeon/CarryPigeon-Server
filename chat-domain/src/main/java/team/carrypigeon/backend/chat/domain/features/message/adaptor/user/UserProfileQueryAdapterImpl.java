package team.carrypigeon.backend.chat.domain.features.message.adaptor.user;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.UserProfileQueryAdapter;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.UserProfileApi;

/**
 * 基于 user feature 的发送者资料适配器。
 * 职责：把 user feature 的用户资料投影翻译为消息视图需要的发送者展示名称。
 * 边界：只做读取与字段裁剪，不缓存、不写资料、不向域内泄漏 user feature 类型。
 */
@Component
public class UserProfileQueryAdapterImpl implements UserProfileQueryAdapter {

    private final UserProfileApi userProfileApi;

    public UserProfileQueryAdapterImpl(UserProfileApi userProfileApi) {
        this.userProfileApi = userProfileApi;
    }

    @Override
    public String findSenderName(long accountId) {
        return userProfileApi.findById(accountId).username();
    }
}
