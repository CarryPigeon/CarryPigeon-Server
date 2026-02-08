package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Request body for {@code PATCH /api/channels/{cid}}.
 * <p>
 * All fields are optional; absent fields are not modified.
 */
public record ChannelPatchRequest(
        String name,
        String brief,
        String avatar
) {
}

