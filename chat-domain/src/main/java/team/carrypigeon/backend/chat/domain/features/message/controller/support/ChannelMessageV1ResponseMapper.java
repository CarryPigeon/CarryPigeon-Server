package team.carrypigeon.backend.chat.domain.features.message.controller.support;

import java.util.Locale;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageV1Response;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * canonical 消息 Wire 映射器。
 * 职责：把领域结果无损转换为统一 HTTP 消息结构。
 * 边界：不解析或重建 data，不补充发送者资料，不承载业务校验。
 */
@Component
public class ChannelMessageV1ResponseMapper {

    /**
     * 将 canonical 领域消息映射为 v1 Wire 响应。
     *
     * @param result canonical 消息结果
     * @return v1 消息响应
     */
    public ChannelMessageV1Response toResponse(ChannelMessageResult result) {
        return new ChannelMessageV1Response(
                Ids.toString(result.messageId()),
                Ids.toString(result.senderId()),
                Ids.toString(result.channelId()),
                result.domain(),
                result.domainVersion(),
                result.data(),
                result.sendTime().toEpochMilli(),
                result.mentions().stream().map(Ids::toString).toList(),
                result.preview(),
                result.status().name().toLowerCase(Locale.ROOT)
        );
    }
}
