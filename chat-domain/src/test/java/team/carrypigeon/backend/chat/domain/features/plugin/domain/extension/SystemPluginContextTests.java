package team.carrypigeon.backend.chat.domain.features.plugin.domain.extension;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifest;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SYSTEM 插件主 Context 访问契约测试。
 * 职责：验证插件生命周期可以从宿主主 Spring Context 获取数据库与事务等核心 Bean。
 */
class SystemPluginContextTests {

    /**
     * 验证全权限插件可以通过统一生命周期上下文获取 DataSource 和事务执行器。
     */
    @Test
    void requireBean_hostResources_returnsMainContextBeans() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        DataSource dataSource = mock(DataSource.class);
        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        when(applicationContext.getBean(DataSource.class)).thenReturn(dataSource);
        when(applicationContext.getBean(TransactionRunner.class)).thenReturn(transactionRunner);
        SystemPluginContext context = new SystemPluginContext(applicationContext, manifest());

        assertSame(dataSource, context.requireBean(DataSource.class));
        assertSame(transactionRunner, context.requireBean(TransactionRunner.class));
    }

    private PluginManifest manifest() {
        return new PluginManifest(
                "plugin-a",
                "Plugin A",
                "1.0.0",
                "test",
                "SYSTEM",
                "STARTUP_CLASSPATH",
                new PluginManifest.PluginHostRequirement("1.0.0", "build", "21", "3.5.3"),
                List.of("test.PluginAutoConfiguration"),
                List.of(),
                List.of(),
                List.of("CarryPigeon:chat-domain:1.0.0"),
                false,
                "cp.plugin.configs.plugin-a",
                "test",
                "checksum"
        );
    }
}
