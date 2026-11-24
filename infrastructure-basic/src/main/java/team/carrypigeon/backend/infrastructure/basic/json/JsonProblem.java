package team.carrypigeon.backend.infrastructure.basic.json;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

public enum JsonProblem implements ProblemMapping {
    JSON_SERIALIZE_FAILED(1001, "JSON Serialize Failed"),
    JSON_DESERIALIZE_FAILED(1002, "JSON Deserialize Failed"),
    ;
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
