package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for {@code GET /api/users/{uid}}.
 */
public record UserIdRequest(String uid) {
}

