package team.carrypigeon.backend.chat.domain.controller.netty.heartbeat;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

@Slf4j
@CPControllerTag("HeartBeat")
public class CPHeartBeatController implements CPController {
    @Override
    public CPResponse process(JsonNode data, CPSession channel) {
        log.debug("HeartBeat");
        return null;
    }
}
