package team.carrypigeon.backend.api.bo.domain.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelTypeBO {
    private ChannelTypeMenu type;
    private String typeName;
}
