package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Request body for {@code PATCH /api/users/me}.
 * <p>
 * This endpoint updates current user's profile fields.
 * All fields are optional, but at least one field must be present.
 */
public record UserMePatchRequest(
        /**
         * New nickname/username.
         */
        String nickname,
        /**
         * New avatar file share key (from {@code POST /api/files/uploads}).
         * <p>
         * Use empty string to clear avatar.
         */
        String avatar
) {
}

