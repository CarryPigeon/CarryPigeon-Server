package team.carrypigeon.backend.chat.domain.controller.user;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.common.response.CPResponse;

@CPControllerTag("")
public class LoginController implements CPController {
    @Override
    public CPResponse process(JsonNode data) {
        return null;
    }
}
