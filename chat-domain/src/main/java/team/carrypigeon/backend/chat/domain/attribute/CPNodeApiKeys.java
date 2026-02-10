package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

/**
 * Keys used by HTTP/WS API layer.
 */
public final class CPNodeApiKeys {

    /** {@code String}: Idempotency key for message creation (from HTTP header {@code Idempotency-Key}). */
    public static final CPKey<String> IDEMPOTENCY_KEY = CPKey.of("Api_IdempotencyKey", String.class);

    /**
     * 工具类不允许实例化。
     */
    private CPNodeApiKeys() {
    }
}

