package team.carrypigeon.backend.chat.domain.shared.result;

/** HTTP 错误体。客户端按五位整数 code 判断结果，message 仅用于展示。 */
public record ErrorResult(int code, String message) implements Result {

    public ErrorResult {
        if (code < 10000 || code > 99999 || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Expected a five-digit error code and a message");
        }
    }
}
