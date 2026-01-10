package team.carrypigeon.backend.chat.domain.plugin;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.plugin.CPPlugin;
import team.carrypigeon.backend.api.starter.server.ServerInfoConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPPluginManagerTests {

    @Test
    void initPlugins_whenNoPlugins_shouldReturn() {
        CPPluginManager manager = new CPPluginManager(null, null);
        assertDoesNotThrow(manager::initPlugins);
    }

    @Test
    void initPlugins_whenPluginsPresent_shouldInitAll() {
        CPPlugin plugin = mock(CPPlugin.class);
        when(plugin.getName()).thenReturn("p");
        when(plugin.getVersion()).thenReturn("1");

        ServerInfoConfig config = new ServerInfoConfig();
        config.setServerName("s");

        CPPluginManager manager = new CPPluginManager(List.of(plugin), config);
        manager.initPlugins();

        verify(plugin, times(1)).init();
    }

    @Test
    void initPlugins_whenPluginInitThrows_shouldContinue() {
        CPPlugin bad = mock(CPPlugin.class);
        when(bad.getName()).thenReturn("bad");
        when(bad.getVersion()).thenReturn("1");
        doThrow(new RuntimeException("boom")).when(bad).init();

        CPPlugin good = mock(CPPlugin.class);
        when(good.getName()).thenReturn("good");
        when(good.getVersion()).thenReturn("2");

        CPPluginManager manager = new CPPluginManager(List.of(bad, good), null);
        manager.initPlugins();

        verify(bad, times(1)).init();
        verify(good, times(1)).init();
    }
}

