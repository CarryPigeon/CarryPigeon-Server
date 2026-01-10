package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelApplicationStateSetterSwitcherNodeTests {

    @Test
    void process_argsMissing_shouldThrowArgsError() {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPFlowContext context = new CPFlowContext();
        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("error args", response.getData().get("msg").asText());
    }

    @Test
    void process_invalidStateValue_shouldThrowArgsError() {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, new CPChannelApplication());
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 3);
        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    @Test
    void process_pendingState_shouldThrowArgsError() {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, new CPChannelApplication());
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 0);
        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    @Test
    void process_approved_shouldSetStateAndTag() throws Exception {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPChannelApplication app = new CPChannelApplication().setState(CPChannelApplicationStateEnum.PENDING);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, app);
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 1);

        node.process(null, context);

        assertEquals(CPChannelApplicationStateEnum.APPROVED, app.getState());
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertEquals("approved", result.msg());
    }

    @Test
    void process_rejected_shouldSetStateAndTag() throws Exception {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPChannelApplication app = new CPChannelApplication().setState(CPChannelApplicationStateEnum.PENDING);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, app);
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 2);

        node.process(null, context);

        assertEquals(CPChannelApplicationStateEnum.REJECTED, app.getState());
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertEquals("rejected", result.msg());
    }
}

