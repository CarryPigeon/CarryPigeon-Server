package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMessageIdsRequest {
    private String[] messageIds;
}
