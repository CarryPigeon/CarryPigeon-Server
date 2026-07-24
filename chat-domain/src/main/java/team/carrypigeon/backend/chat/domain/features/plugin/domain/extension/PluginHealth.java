package team.carrypigeon.backend.chat.domain.features.plugin.domain.extension;

/**
 * 插件健康状态。
 * 职责：表达插件启动后是否可以继续提供能力。
 */
public record PluginHealth(Status status, String message) {

    public PluginHealth {
        status = status == null ? Status.FAILED : status;
        message = message == null ? "" : message;
    }

    public static PluginHealth active() {
        return new PluginHealth(Status.ACTIVE, "");
    }

    public static PluginHealth degraded(String message) {
        return new PluginHealth(Status.DEGRADED, message);
    }

    public static PluginHealth failed(String message) {
        return new PluginHealth(Status.FAILED, message);
    }

    public enum Status {
        ACTIVE,
        DEGRADED,
        FAILED
    }
}
