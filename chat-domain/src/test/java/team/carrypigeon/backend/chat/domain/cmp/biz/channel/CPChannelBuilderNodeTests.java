package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelBuilderNodeTests {

    @Test
    void process_shouldBuildChannelAndWriteToContext() throws Exception {
        CPChannelBuilderNode node = new CPChannelBuilderNode();

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_NAME, "c");
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_OWNER, 2L);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_BRIEF, "b");
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_AVATAR, 3L);
        long nowMillis = 1700000000000L;
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_CREATE_TIME, nowMillis);

        node.process(null, context);

        CPChannel channel = context.getData(CPNodeChannelKeys.CHANNEL_INFO);
        assertNotNull(channel);
        assertEquals(1L, channel.getId());
        assertEquals("c", channel.getName());
        assertEquals(2L, channel.getOwner());
        assertEquals("b", channel.getBrief());
        assertEquals(3L, channel.getAvatar());
        assertEquals(TimeUtil.MillisToLocalDateTime(nowMillis), channel.getCreateTime());
    }

    @Test
    void process_missingRequiredKey_shouldThrowAndSetArgsError() {
        CPChannelBuilderNode node = new CPChannelBuilderNode();

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_NAME, "c");

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }
}

