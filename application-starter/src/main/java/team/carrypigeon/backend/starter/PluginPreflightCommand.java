package team.carrypigeon.backend.starter;

import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginHostIdentity;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestCatalog;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestLoader;

/**
 * 分发包插件预检命令入口。
 * 职责：复用正式启动预检，在不创建 Spring Context、不连接外部服务的情况下验证当前插件 classpath。
 * 边界：不修改 classpath、不执行业务 Bean 或插件生命周期；插件目录是否进入 classpath 由启动脚本决定。
 */
public final class PluginPreflightCommand {

    private PluginPreflightCommand() {
    }

    /**
     * 执行当前 JVM classpath 的插件预检并输出稳定摘要。
     *
     * @param args 不接受命令参数
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("PluginPreflightCommand does not accept arguments");
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        PluginManifestCatalog catalog = verify(classLoader);
        PluginHostIdentity host = PluginHostIdentity.load(classLoader);
        System.out.printf(
                "Plugin preflight passed: hostVersion=%s, buildHash=%s, springBootVersion=%s, pluginCount=%d%n",
                host.version(),
                host.buildHash(),
                host.springBootVersion(),
                catalog.manifests().size()
        );
        catalog.manifests().forEach(manifest -> System.out.printf(
                "Plugin verified: pluginId=%s, version=%s, sha256=%s%n",
                manifest.pluginId(),
                manifest.version(),
                manifest.sha256()
        ));
    }

    /**
     * 对指定 ClassLoader 执行与正式启动一致的预检。
     *
     * @param classLoader 待验证的启动 ClassLoader
     * @return 已通过预检的插件目录
     */
    static PluginManifestCatalog verify(ClassLoader classLoader) {
        PluginHostIdentity host = PluginHostIdentity.load(classLoader);
        return PluginManifestLoader.load(classLoader, host);
    }
}
