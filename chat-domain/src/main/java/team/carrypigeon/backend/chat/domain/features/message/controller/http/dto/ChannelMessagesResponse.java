package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMessagesResponse {
    private int total;
    private Collection<ChannelMessageResponse> items;

    public static ChannelMessagesResponse of(Collection<ChannelMessageResult> items) {
        return new ChannelMessagesResponse(items.size(), items.stream().map(ChannelMessageResponse::from).collect(Collectors.toList()));
    }
}
