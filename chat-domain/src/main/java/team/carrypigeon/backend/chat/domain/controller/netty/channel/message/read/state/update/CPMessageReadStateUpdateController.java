package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.update;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * Update channel message read state for current user.<br/>
 * url: /core/channel/message/read/state/update<br/>
 * Request: {@link CPMessageReadStateUpdateVO}<br/>
 * Response: default success result.<br/>
 */
@CPControllerTag(
        path = "/core/channel/message/read/state/update",
        voClazz = CPMessageReadStateUpdateVO.class,
        resultClazz = CPControllerDefaultResult.class
)
public class CPMessageReadStateUpdateController {
}

