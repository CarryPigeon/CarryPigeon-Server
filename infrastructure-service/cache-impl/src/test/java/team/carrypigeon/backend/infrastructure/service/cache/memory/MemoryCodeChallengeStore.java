package team.carrypigeon.backend.infrastructure.service.cache.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CodeChallengeStore;

/** 内存挑战保留消费、失败和耗尽状态至到期，同键重发后旧邮件的回调只操作自己的批次。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryCodeChallengeStore implements CodeChallengeStore {
    private static class Challenge {
        final String id;
        final Instant expiresAt;
        final String code;
        int remaining;
        Result terminal;

        Challenge(String id, String code, int remaining, Instant expiresAt) {
            this.id = id;
            this.code = code;
            this.remaining = remaining;
            this.expiresAt = expiresAt;
        }
    }

    private final Clock clock;
    private final Map<String, Challenge> challenges = new HashMap<>();

    public MemoryCodeChallengeStore(Clock clock) {
        this.clock = clock;
    }

    private Challenge current(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("challenge key must not be blank");
        }
        Challenge challenge = challenges.get(key);
        if (challenge != null && !clock.instant().isBefore(challenge.expiresAt)) {
            challenges.remove(key);
            return null;
        }
        return challenge;
    }

    @Override
    public synchronized void replace(String key, String challengeId, String code, int attempts, Duration ttl) {
        current(key);
        if (challengeId == null || challengeId.isBlank() || code == null || code.isBlank()
                || attempts <= 0 || ttl == null || ttl.toMillis() <= 0) {
            throw new IllegalArgumentException("challenge ID, code, attempts and TTL must be valid");
        }
        challenges.put(key, new Challenge(challengeId, code, attempts, clock.instant().plus(ttl)));
    }

    @Override
    public synchronized Result verify(String key, String code) {
        Objects.requireNonNull(code);
        Challenge challenge = current(key);
        if (challenge == null) {
            return Result.UNAVAILABLE;
        }
        if (challenge.terminal != null) {
            return challenge.terminal;
        }
        if (challenge.code.equals(code)) {
            challenge.terminal = Result.CONSUMED;
            return Result.VERIFIED;
        }
        if (--challenge.remaining == 0) {
            challenge.terminal = Result.EXHAUSTED;
            return Result.EXHAUSTED;
        }
        return Result.INVALID;
    }

    @Override
    public synchronized void completeDelivery(String key, String challengeId, boolean accepted) {
        if (challengeId == null || challengeId.isBlank()) {
            throw new IllegalArgumentException("challenge ID must not be blank");
        }
        Challenge challenge = current(key);
        if (!accepted && challenge != null && challenge.id.equals(challengeId) && challenge.terminal == null) {
            challenge.terminal = Result.UNAVAILABLE;
        }
    }
}
