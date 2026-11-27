package team.carrypigeon.backend.infrastructure.basic.json;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

public enum JsonProblem implements ProblemMapping {
    JSON_SERIALIZE_FAILED(10012, "internal server error"),
    JSON_DESERIALIZE_FAILED(10012, "internal server error");
    private final int code;
    private final String message;

    JsonProblem(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
