package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * One installed plugin descriptor reported by the client.
 * <p>
 * JSON fields: {@code plugin_id}, {@code version}.
 */
public record InstalledPlugin(@NotBlank String pluginId, String version) {
}
