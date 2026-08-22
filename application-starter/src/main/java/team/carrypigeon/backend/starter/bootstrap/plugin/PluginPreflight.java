package team.carrypigeon.backend.starter.bootstrap.plugin;

import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginHostIdentity;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestCatalog;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestLoader;

/**
 * 插件启动预检入口。
 * 职责：一次性加载宿主身份并据此校验当前启动 classpath 中的插件 Manifest。
 * 边界：不创建 Spring Context，不连接外部服务，也不修改当前 classpath。
 */
public final class PluginPreflight {

    private PluginPreflight() {
    }

    /**
     * 对指定 ClassLoader 执行插件预检。
     *
     * @param classLoader 待验证的启动 ClassLoader
     * @return 包含宿主身份与已验证插件目录的不可变结果
     */
    public static Result verify(ClassLoader classLoader) {
        PluginHostIdentity hostIdentity = PluginHostIdentity.load(classLoader);
        PluginManifestCatalog manifestCatalog = PluginManifestLoader.load(classLoader, hostIdentity);
        return new Result(hostIdentity, manifestCatalog);
    }

    /**
     * 插件预检结果。
     *
     * @param hostIdentity 当前宿主构建身份
     * @param manifestCatalog 已通过预检的插件目录
     */
    public record Result(
            PluginHostIdentity hostIdentity,
            PluginManifestCatalog manifestCatalog
    ) {
    }
}
