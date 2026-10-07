package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.BanChannelMemberUntilCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.UnbanChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.result.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input.ListChannelBansQuery;
import java.util.List;

/**
 * 频道禁言领域 API。
 * 职责：暴露频道成员禁言、限时禁言与解除禁言能力。
 * 边界：不暴露 HTTP 协议、具体实现类或禁言仓储细节。
 */
public interface ChannelBanApi {

    List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query);

    ChannelBanResult banChannelMember(BanChannelMemberCommand command);

    ChannelBanResult banChannelMemberUntil(BanChannelMemberUntilCommand command);

    ChannelBanResult unbanChannelMember(UnbanChannelMemberCommand command);
}
