package team.carrypigeon.backend.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDispatcher;
import team.carrypigeon.backend.api.starter.connection.ConnectionConfig;
import team.carrypigeon.backend.connection.handler.ConnectionHandler;
import team.carrypigeon.backend.connection.heart.CPNettyHeartBeatHandler;
import team.carrypigeon.backend.connection.protocol.codec.NettyDecoder;
import team.carrypigeon.backend.connection.protocol.codec.NettyEncoder;
import team.carrypigeon.backend.connection.security.EccServerKeyHolder;

import java.lang.reflect.Field;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NettyConnectionStarterTests {

    @Test
    void startAndStop_withEphemeralPort_shouldNotHang() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0); // let OS pick an ephemeral port

        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);

        starter.start();

        // Wait until server thread is created, then stop.
        Thread serverThread = getField(starter, "serverThread", Thread.class);
        assertNotNull(serverThread);

        Thread.sleep(200);

        assertDoesNotThrow(starter::stop);
        serverThread.join(5000);
    }

    @Test
    void stop_withoutStart_shouldNotThrow() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);
        EccServerKeyHolder holder = new EccServerKeyHolder(config);

        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);
        assertDoesNotThrow(starter::stop);
    }

    @Test
    void initChildPipeline_shouldAddHandlers() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);

        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);

        EventExecutorGroup bizGroup = new DefaultEventExecutorGroup(1);
        EmbeddedChannel channel = new EmbeddedChannel();
        try {
            assertDoesNotThrow(() -> starter.initChildPipeline(channel.pipeline(), bizGroup));
            assertNotNull(channel.pipeline().get(NettyDecoder.class));
            assertNotNull(channel.pipeline().get(NettyEncoder.class));
            assertNotNull(channel.pipeline().get(IdleStateHandler.class));
            assertNotNull(channel.pipeline().get(CPNettyHeartBeatHandler.class));
            assertNotNull(channel.pipeline().get(ConnectionHandler.class));
        } finally {
            channel.finishAndReleaseAll();
            bizGroup.shutdownGracefully();
        }
    }

    @Test
    void childChannelInitializer_shouldDelegateToInitChildPipeline() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);
        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);

        EventExecutorGroup bizGroup = new DefaultEventExecutorGroup(1);
        EmbeddedChannel embedded = new EmbeddedChannel();
        SocketChannel socketChannel = mock(SocketChannel.class);
        when(socketChannel.pipeline()).thenReturn(embedded.pipeline());

        try {
            NettyConnectionStarter.ChildChannelInitializer initializer =
                    new NettyConnectionStarter.ChildChannelInitializer(starter, bizGroup);
            assertDoesNotThrow(() -> initializer.initChannel(socketChannel));
            assertNotNull(embedded.pipeline().get(ConnectionHandler.class));
        } finally {
            embedded.finishAndReleaseAll();
            bizGroup.shutdownGracefully();
        }
    }

    @Test
    void stop_withNonNullServerChannel_shouldCloseWithoutException() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);
        EccServerKeyHolder holder = new EccServerKeyHolder(config);

        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);

        Channel serverChannel = mock(Channel.class);
        when(serverChannel.close()).thenReturn(null);
        setField(starter, "serverChannel", serverChannel);
        setField(starter, "serverThread", new Thread(() -> {}));

        assertDoesNotThrow(starter::stop);
        verify(serverChannel, times(1)).close();
    }

    @Test
    void run_whenThreadInterrupted_shouldExitWithoutHanging() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);

        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder) {
            @Override
            ChannelFuture bind(ServerBootstrap bootstrap) throws InterruptedException {
                throw new InterruptedException("test");
            }
        };

        assertFalse(Thread.currentThread().isInterrupted());
        starter.run();
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    @Test
    void run_whenBindSucceeds_shouldAwaitCloseFuture() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);

        EccServerKeyHolder holder = new EccServerKeyHolder(config);

        ChannelFuture closeFuture = mock(ChannelFuture.class);
        when(closeFuture.sync()).thenReturn(closeFuture);
        Channel channel = mock(Channel.class);
        when(channel.closeFuture()).thenReturn(closeFuture);

        ChannelFuture bindFuture = mock(ChannelFuture.class);
        when(bindFuture.channel()).thenReturn(channel);

        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder) {
            @Override
            ChannelFuture bind(ServerBootstrap bootstrap) {
                return bindFuture;
            }
        };

        assertDoesNotThrow(starter::run);
        verify(channel, times(1)).closeFuture();
        verify(closeFuture, times(1)).sync();
    }

    @Test
    void run_whenBindThrowsRuntimeException_shouldReturnNormally() {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);

        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder) {
            @Override
            ChannelFuture bind(ServerBootstrap bootstrap) {
                throw new RuntimeException("boom");
            }
        };

        assertDoesNotThrow(starter::run);
    }

    @Test
    void bind_shouldCallBootstrapBindAndSync() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);
        EccServerKeyHolder holder = new EccServerKeyHolder(config);

        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);

        ServerBootstrap bootstrap = mock(ServerBootstrap.class);
        ChannelFuture future = mock(ChannelFuture.class);
        when(bootstrap.bind()).thenReturn(future);
        when(future.sync()).thenReturn(future);

        assertSame(future, starter.bind(bootstrap));
        verify(bootstrap, times(1)).bind();
        verify(future, times(1)).sync();
    }

    @Test
    void stop_whenResourcesThrow_shouldSwallowExceptions() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);
        EccServerKeyHolder holder = new EccServerKeyHolder(config);

        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);

        Channel badChannel = mock(Channel.class);
        doThrow(new RuntimeException("boom")).when(badChannel).close();
        setField(starter, "serverChannel", badChannel);

        EventLoopGroup boss = mock(EventLoopGroup.class);
        EventLoopGroup worker = mock(EventLoopGroup.class);
        EventExecutorGroup biz = mock(EventExecutorGroup.class);
        doThrow(new RuntimeException("boom")).when(boss).shutdownGracefully();
        doThrow(new RuntimeException("boom")).when(worker).shutdownGracefully();
        doThrow(new RuntimeException("boom")).when(biz).shutdownGracefully();
        setField(starter, "bossGroup", boss);
        setField(starter, "workerGroup", worker);
        setField(starter, "bizGroup", biz);

        setField(starter, "serverThread", new Thread(() -> {}));
        assertDoesNotThrow(starter::stop);
    }

    @Test
    void stop_withGroups_shouldShutdownGracefully() throws Exception {
        CPControllerDispatcher dispatcher = mock(CPControllerDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConnectionConfig config = new ConnectionConfig();
        config.setPort(0);
        EccServerKeyHolder holder = new EccServerKeyHolder(config);

        NettyConnectionStarter starter = new NettyConnectionStarter(dispatcher, objectMapper, config, holder);

        EventLoopGroup boss = mock(EventLoopGroup.class);
        EventLoopGroup worker = mock(EventLoopGroup.class);
        EventExecutorGroup biz = mock(EventExecutorGroup.class);
        when(boss.shutdownGracefully()).thenReturn(null);
        when(worker.shutdownGracefully()).thenReturn(null);
        when(biz.shutdownGracefully()).thenReturn(null);

        setField(starter, "bossGroup", boss);
        setField(starter, "workerGroup", worker);
        setField(starter, "bizGroup", biz);
        setField(starter, "serverThread", new Thread(() -> {}));

        assertDoesNotThrow(starter::stop);
        verify(boss, times(1)).shutdownGracefully();
        verify(worker, times(1)).shutdownGracefully();
        verify(biz, times(1)).shutdownGracefully();
    }

    @Test
    void eccUtil_generateKeyPair_shouldCoverErrorPathWhenProviderMissing() {
        var bc = Security.getProvider("BC");
        if (bc == null) {
            // Provider may not be installed in some environments; ECCUtil installs it in static init.
            // If it's still missing, skip this behavior-driven test.
            return;
        }

        try {
            Security.removeProvider("BC");
            assertThrows(RuntimeException.class, team.carrypigeon.backend.connection.protocol.encryption.ecc.ECCUtil::generateEccKeyPair);
        } finally {
            Security.addProvider(bc);
        }
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
