package team.carrypigeon.backend.chat.domain.cmp.api.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * Ensure decision request {@code cid} matches the application's {@code cid}.
 */
@Slf4j
@LiteflowComponent("ApiChannelApplicationCidMatchGuard")
public class ApiChannelApplicationCidMatchGuardNode extends CPNodeComponent {

    @Override
    protected void process(CPFlowContext context) {
        Long requestCid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        CPChannelApplication app = requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO);
        Long appCid = app.getCid();
        if (appCid == null || !appCid.equals(requestCid)) {
            log.info("ApiChannelApplicationCidMatchGuard mismatch, requestCid={}, appCid={}", requestCid, app.getCid());
            fail(CPProblem.of(422, "validation_failed", "validation failed"));
        }
    }
}
