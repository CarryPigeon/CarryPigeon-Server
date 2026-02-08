package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import java.util.List;

/**
 * Internal request object used by LiteFlow for {@code GET /api/users?ids=...}.
 */
public record UsersBatchRequest(List<String> ids) {
}

