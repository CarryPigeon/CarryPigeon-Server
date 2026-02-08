package team.carrypigeon.backend.api.chat.domain.error;

/**
 * Machine-readable error for API requests.
 * <p>
 * This is transport-neutral. HTTP layer can map it to status+JSON body, and other transports can map it to their own
 * envelopes.
 */
public record CPProblem(
        int status,
        String reason,
        String message,
        Object details
) {
    public static CPProblem of(int status, String reason, String message) {
        return new CPProblem(status, reason, message, null);
    }

    public static CPProblem of(int status, String reason, String message, Object details) {
        return new CPProblem(status, reason, message, details);
    }
}

