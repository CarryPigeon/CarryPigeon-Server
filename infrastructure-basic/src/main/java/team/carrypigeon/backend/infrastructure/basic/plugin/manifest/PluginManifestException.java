package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

/**
 * 插件 Manifest 或启动前校验失败异常。
 * 职责：表达插件集合不能安全进入 Spring Context 的启动失败。
 */
public class PluginManifestException extends IllegalStateException {

    public PluginManifestException(String message) {
        super(message);
    }

    public PluginManifestException(String message, Throwable cause) {
        super(message, cause);
    }
}
