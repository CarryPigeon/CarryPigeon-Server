package team.carrypigeon.backend.chat.domain.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** 成功响应；撤回、删除等无返回值操作也必须写出 data: null。 */
@JsonPropertyOrder({"code", "message", "data"})
public record SuccessResult<T>(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    T data
) implements Result<T> {

    @JsonProperty("code")
    public int code() {
        return 0;
    }

    @JsonProperty("message")
    public String message() {
        return "OK";
    }
}
