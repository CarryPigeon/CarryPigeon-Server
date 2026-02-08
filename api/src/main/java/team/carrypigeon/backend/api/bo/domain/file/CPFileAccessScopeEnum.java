package team.carrypigeon.backend.api.bo.domain.file;

import lombok.extern.slf4j.Slf4j;

/**
 * File access scope.
 * <p>
 * This scope controls who can download a file from {@code GET /api/files/download/{share_key}}.
 * The scope is stored in {@code file_info.access_scope}.
 */
@Slf4j
public enum CPFileAccessScopeEnum {
    /**
     * Only the uploader (owner) can download.
     */
    OWNER,
    /**
     * Any authenticated user can download.
     * <p>
     * This is typically used for user/channel avatars inside the app.
     */
    AUTH,
    /**
     * Only members of the target channel can download.
     * <p>
     * Requires {@code file_info.scope_cid > 0}.
     */
    CHANNEL,
    /**
     * Anyone (including anonymous clients) can download.
     */
    PUBLIC;

    /**
     * Parse an access scope string.
     * <p>
     * Unknown values fallback to {@link #OWNER} and are logged, to avoid failing the whole request when legacy/bad data
     * is present in database.
     */
    public static CPFileAccessScopeEnum parseOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return OWNER;
        }
        try {
            return CPFileAccessScopeEnum.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            log.error("File access scope parse failed, value={}", value);
            return OWNER;
        }
    }
}

