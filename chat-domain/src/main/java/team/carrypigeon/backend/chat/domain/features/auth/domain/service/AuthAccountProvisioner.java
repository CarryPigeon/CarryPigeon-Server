package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 鉴权账号开通协作对象。
 * 职责：在账号创建后补齐用户资料、默认频道成员关系和系统频道成员关系。
 * 边界：只服务 auth 注册与邮箱验证码建号链路，不负责 token 签发或登录校验。
 */
class AuthAccountProvisioner {

    private final UserProfileRepository userProfileRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final TimeProvider timeProvider;

    AuthAccountProvisioner(
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            TimeProvider timeProvider
    ) {
        this.userProfileRepository = userProfileRepository;
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.timeProvider = timeProvider;
    }

    void provisionAccount(AuthAccount account, String nickname) {
        userProfileRepository.save(UserProfile.initial(
                account.id(),
                nickname,
                account.createdAt(),
                account.updatedAt()
        ));
        Channel defaultChannel = channelRepository.findDefaultChannel()
                .orElseThrow(() -> ProblemException.fail("default_channel_missing", "default channel does not exist"));
        channelMemberRepository.save(new ChannelMember(
                defaultChannel.id(),
                account.id(),
                ChannelMemberRole.MEMBER,
                timeProvider.nowInstant(),
                null
        ));
        Channel systemChannel = channelRepository.findSystemChannel()
                .orElseThrow(() -> ProblemException.fail("system_channel_missing", "system channel does not exist"));
        if (!channelMemberRepository.exists(systemChannel.id(), account.id())) {
            channelMemberRepository.save(new ChannelMember(
                    systemChannel.id(),
                    account.id(),
                    ChannelMemberRole.MEMBER,
                    timeProvider.nowInstant(),
                    null
            ));
        }
    }
}
