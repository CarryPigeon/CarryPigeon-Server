package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelApplicationStateSetterSwitcherNodeTests {

    @Test
    void process_argsMissing_shouldThrowArgsError() {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void process_invalidStateValue_shouldThrowArgsError() {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, new CPChannelApplication());
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 3);
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void process_pendingState_shouldThrowArgsError() {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, new CPChannelApplication());
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 0);
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason());
    }

    @Test
    void process_approved_shouldSetStateAndTag() throws Exception {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPChannelApplication app = new CPChannelApplication().setState(CPChannelApplicationStateEnum.PENDING);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, app);
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 1);

        node.process(null, context);

        assertEquals(CPChannelApplicationStateEnum.APPROVED, app.getState());
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertEquals("approved", result.msg());
    }

    @Test
    void process_rejected_shouldSetStateAndTag() throws Exception {
        CPChannelApplicationStateSetterSwitcherNode node = new CPChannelApplicationStateSetterSwitcherNode();
        CPChannelApplication app = new CPChannelApplication().setState(CPChannelApplicationStateEnum.PENDING);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO, app);
        context.set(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, 2);

        node.process(null, context);

        assertEquals(CPChannelApplicationStateEnum.REJECTED, app.getState());
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertEquals("rejected", result.msg());
    }
}
