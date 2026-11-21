package team.carrypigeon.backend.distribution;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.file.config.FileShareKeyProperties;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.infrastructure.service.cache.impl.config.RedisCacheProperties;
import team.carrypigeon.backend.infrastructure.service.database.impl.config.DatabaseServiceProperties;
import team.carrypigeon.backend.infrastructure.service.mail.impl.config.SmtpMailProperties;
import team.carrypigeon.backend.infrastructure.service.storage.impl.config.MinioStorageProperties;

/**
 * 分发包离线配置检查入口。
 * 使用 Spring 的配置加载与现有属性绑定规则，不创建容器、连接外部服务或模拟插件预检。
 */
public final class DistributionVerifier {

    private DistributionVerifier() {
    }

    /**
     * 默认检查 YAML 加载，--strict-config 额外检查应用已有的关键配置契约。
     * 其余参数按 Spring 命令行配置处理；失败只输出配置前缀，避免异常携带密钥值。
     */
    public static void main(String[] args) {
        boolean strict = false;
        List<String> properties = new ArrayList<>();
        for (String arg : args) {
            if ("--strict-config".equals(arg)) {
                strict = true;
            } else if (arg.startsWith("--") && arg.contains("=")) {
                properties.add(arg);
            } else {
                System.err.println("Unsupported verifier argument; use --strict-config or --property=value.");
                System.exit(1);
                return;
            }
        }
        try {
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource(properties.toArray(String[]::new)));
            // 与正常启动保持相同的 YAML、profile、环境变量和命令行覆盖顺序。
            ConfigDataEnvironmentPostProcessor.applyTo(environment);
            if (strict) {
                Binder binder = Binder.get(environment);
                var jwt = binder.bind("cp.chat.auth.jwt", AuthJwtProperties.class).get();
                var file = binder.bind("cp.chat.file.share-key", FileShareKeyProperties.class).get();
                new SigningKeyValidation(environment, jwt, file);
                binder.bind("cp.chat.server.realtime", RealtimeServerProperties.class).get().afterPropertiesSet();
                binder.bind("service.database.mysql", DatabaseServiceProperties.class).get();
                binder.bind("service.cache.redis", RedisCacheProperties.class).get();
                binder.bind("service.storage.minio", MinioStorageProperties.class).get();
                binder.bind("service.mail.smtp", SmtpMailProperties.class).get();
            }
            System.out.println(strict ? "Distribution configuration verified (offline)." : "Distribution files and YAML verified (offline).");
        } catch (BindException exception) {
            System.err.println("Configuration validation failed at: " + exception.getName());
            System.exit(1);
        } catch (RuntimeException exception) {
            System.err.println("Configuration loading failed (" + exception.getClass().getSimpleName() + ").");
            System.exit(1);
        }
    }
}
