package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Client metadata attached to auth-related requests.
 * <p>
 * JSON fields use snake_case (configured by {@code spring.jackson.property-naming-strategy=SNAKE_CASE}):
 * {@code device_id}, {@code installed_plugins}.
 */
public record ClientInfo(@NotBlank String deviceId, @Valid List<InstalledPlugin> installedPlugins) {
}
