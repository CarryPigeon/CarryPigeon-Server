package team.carrypigeon.backend.connection.heart;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPNettyHeartBeatHandlerTests {

    @Test
    void writerIdle_withSession_shouldSendHeartbeatPlain() {
        CPSession session = mock(CPSession.class);
        EmbeddedChannel channel = new EmbeddedChannel(new CPNettyHeartBeatHandler());
        channel.attr(ConnectionAttributes.SESSIONS).set(session);

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);

        verify(session, times(1)).write("", false);
        assertTrue(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void writerIdle_withoutSession_shouldDoNothing() {
        EmbeddedChannel channel = new EmbeddedChannel(new CPNettyHeartBeatHandler());
        channel.attr(ConnectionAttributes.SESSIONS).set(null);

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);

        assertTrue(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void readerIdle_shouldCloseChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(new CPNettyHeartBeatHandler());
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);

        assertFalse(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void allIdle_shouldCloseChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(new CPNettyHeartBeatHandler());
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT);

        assertFalse(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void nonIdleEvent_shouldDelegateToSuper() {
        EmbeddedChannel channel = new EmbeddedChannel(new CPNettyHeartBeatHandler());
        channel.pipeline().fireUserEventTriggered(new Object());
        assertTrue(channel.isActive());
        channel.finishAndReleaseAll();
    }
}
