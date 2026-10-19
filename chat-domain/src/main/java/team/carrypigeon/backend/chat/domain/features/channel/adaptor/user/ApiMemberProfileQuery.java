package team.carrypigeon.backend.chat.domain.features.channel.adaptor.user;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.MemberProfileQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.MemberProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfileByAccountIdQuery;

/**
 * 基于 user feature 的成员展示资料适配器。
 * 职责：把 user feature 的用户资料投影翻译为频道视图需要的成员展示资料。
 * 边界：只做读取与字段裁剪，不缓存、不写资料、不向域内泄漏 user feature 类型。
 */
@Component
public class ApiMemberProfileQuery implements MemberProfileQuery {

    private final UserProfileApi userProfileApi;

    public ApiMemberProfileQuery(UserProfileApi userProfileApi) {
        this.userProfileApi = userProfileApi;
    }

    @Override
    public Map<Long, MemberProfile> findProfiles(Collection<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Map.of();
        }
        return userProfileApi.getPublicUserProfiles(List.copyOf(accountIds)).stream()
                .collect(Collectors.toMap(
                        UserProfileResult::accountId,
                        this::toProfile,
                        (first, ignored) -> first
                ));
    }

    @Override
    public MemberProfile findProfile(long accountId) {
        return toProfile(userProfileApi.getUserProfileByAccountId(new GetUserProfileByAccountIdQuery(accountId)));
    }

    private MemberProfile toProfile(UserProfileResult result) {
        return new MemberProfile(result.accountId(), result.username(), result.avatarUrl());
    }
}
