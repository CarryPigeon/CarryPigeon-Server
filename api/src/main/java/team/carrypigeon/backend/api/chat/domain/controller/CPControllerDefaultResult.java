package team.carrypigeon.backend.api.chat.domain.controller;

import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;

public class CPControllerDefaultResult implements CPControllerResult{
    @Override
    public void process(CPSession session, DefaultContext context) {
        // 空处理
    }
}
