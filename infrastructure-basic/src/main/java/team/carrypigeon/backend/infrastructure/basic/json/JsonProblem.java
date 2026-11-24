package team.carrypigeon.backend.infrastructure.basic.json;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

public enum JsonProblem implements ProblemMapping {
    JSON_SERIALIZE_FAILED(1001, 400, "JSON_SERIALIZE_FAILED", "JSON Serialize Failed"),
    JSON_DESERIALIZE_FAILED(1002, 400, "JSON_DESERIALIZE_FAILED", "JSON Deserialize Failed"),
    ;
    private final int code;
    private final int status;
    private final String reason;
    private final String message;

    JsonProblem(int code, int status, String reason, String message) {
        this.code = code;
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public String message() {
        return message;
    }
}
