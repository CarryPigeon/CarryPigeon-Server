package team.carrypigeon.backend.infrastructure.service.storage.api.exception;

/**
 * 对象存储服务异常。
 * 职责：表达 storage-api 能力执行失败的稳定异常语义。
 * 边界：不直接暴露 MinIO 或具体 SDK 异常类型。
 */
public class StorageServiceException extends RuntimeException {

    public StorageServiceException(String message) {
        super(message);
    }

    public StorageServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
