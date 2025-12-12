package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.get;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * Get channel message read state for current user.<br/>
 * url: /core/channel/message/read/state/get<br/>
 * Request: {@link CPMessageReadStateGetVO}<br/>
 * Response: {@link CPMessageReadStateGetResult}<br/>
 */
@CPControllerTag(
        path = "/core/channel/message/read/state/get",
        voClazz = CPMessageReadStateGetVO.class,
        resultClazz = CPMessageReadStateGetResult.class
)
public class CPMessageReadStateGetController {
}

