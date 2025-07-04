package team.carrypigeon.backend.chat.domain.controller;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.connection.pool.CPChannel;

@Component
public class CPControllerDispatcherImpl implements CPControllerDispatcher {
    @Override
    public void process(CPChannel channel, String msg) {

    }
}
