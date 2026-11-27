package team.carrypigeon.backend.chat.domain.shared.result;

/** HTTP 业务响应。成功时 data 始终存在，失败时只有数字码与英文说明。 */
public sealed interface Result permits SuccessResult, ErrorResult {

    static <T> SuccessResult<T> success(T data) {
        return new SuccessResult<>(data);
    }

    static SuccessResult<Void> success() {
        return success(null);
    }

    static ErrorResult failure(int code, String message) {
        return new ErrorResult(code, message);
    }
}
