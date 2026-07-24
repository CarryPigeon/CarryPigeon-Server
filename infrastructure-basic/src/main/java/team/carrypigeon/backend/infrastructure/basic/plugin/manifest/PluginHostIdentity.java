package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.boot.SpringBootVersion;

/**
 * 宿主运行时身份。
 * 职责：提供插件 Manifest 校验所需的宿主版本、构建指纹和运行时版本。
 * 边界：只描述宿主身份，不负责插件加载或生命周期管理。
 */
public record PluginHostIdentity(
        String version,
        String buildHash,
        String javaVersion,
        String springBootVersion
) {

    private static final String RESOURCE = "META-INF/carrypigeon/host.properties";

    public PluginHostIdentity {
        version = requireValue(version, "version");
        buildHash = requireValue(buildHash, "buildHash");
        javaVersion = requireValue(javaVersion, "javaVersion");
        springBootVersion = requireValue(springBootVersion, "springBootVersion");
    }

    /**
     * 从当前应用 classpath 读取宿主构建信息。
     * 开发环境没有经过 Maven 资源过滤时使用 development 标识，避免伪造发布构建指纹。
     *
     * @param classLoader 当前应用 ClassLoader
     * @return 宿主身份
     */
    public static PluginHostIdentity load(ClassLoader classLoader) {
        Properties properties = new Properties();
        try (InputStream input = classLoader.getResourceAsStream(RESOURCE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read host identity resource", exception);
        }
        return new PluginHostIdentity(
                properties.getProperty("host.version", "development"),
                properties.getProperty("host.build-hash", "development"),
                System.getProperty("java.specification.version", "unknown"),
                SpringBootVersion.getVersion() == null ? "unknown" : SpringBootVersion.getVersion()
        );
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
