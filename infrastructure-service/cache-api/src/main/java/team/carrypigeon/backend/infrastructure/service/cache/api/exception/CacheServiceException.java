package team.carrypigeon.backend.infrastructure.service.cache.api.exception;

/**
 * 缓存服务异常。
 * 职责：表达 cache-api 能力执行失败的稳定异常语义。
 * 边界：不直接暴露 Redis 或 Lettuce 异常类型。
 */
public class CacheServiceException extends RuntimeException {

    public CacheServiceException(String message) {
        super(message);
    }

    public CacheServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
