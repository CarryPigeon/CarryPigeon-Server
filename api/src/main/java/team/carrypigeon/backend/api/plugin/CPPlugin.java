package team.carrypigeon.backend.api.plugin;

/**
 * Core plugin interface for CarryPigeon backend.
 *
 * Implementations of this interface should be provided by plugin JARs
 * under package {@code team.carrypigeon.backend.plugin.*}.
 *
 * All plugin beans will be discovered by Spring (via component scanning)
 * and then managed by the core plugin manager in chat-domain.
 */
public interface CPPlugin {

    /**
     * Human readable name of this plugin.
     */
    String getName();

    /**
     * Version of the plugin, e.g. "1.0.0".
     */
    String getVersion();

    /**
     * Called once on application startup after the Spring context is ready.
     * Use this to perform plugin initialization (e.g. register handlers).
     */
    void init();
}
