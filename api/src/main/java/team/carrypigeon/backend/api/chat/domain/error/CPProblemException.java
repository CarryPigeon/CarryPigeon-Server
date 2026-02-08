package team.carrypigeon.backend.api.chat.domain.error;

import lombok.Getter;

/**
 * Runtime exception carrying a {@link CPProblem}.
 * <p>
 * Intended to be thrown by LiteFlow nodes when executing under API transports (HTTP/WS).
 */
@Getter
public class CPProblemException extends RuntimeException {

    private final CPProblem problem;

    public CPProblemException(CPProblem problem) {
        super(problem == null ? null : problem.message());
        this.problem = problem;
    }
}

