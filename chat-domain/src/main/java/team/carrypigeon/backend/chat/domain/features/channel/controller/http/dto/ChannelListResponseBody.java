package team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto;

import java.util.Collection;

public record ChannelListResponseBody(
    int total,
    Collection<ChannelResponseBody>  items
) {
}
