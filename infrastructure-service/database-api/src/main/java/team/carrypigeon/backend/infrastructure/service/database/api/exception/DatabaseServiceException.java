package team.carrypigeon.backend.infrastructure.service.database.api.exception;

/**
 * 数据库服务异常。
 * 职责：表达 database-api 能力执行失败的稳定异常语义。
 * 边界：不直接暴露 JDBC、SQL 或具体数据库驱动异常。
 */
public class DatabaseServiceException extends RuntimeException {

    public DatabaseServiceException(String message) {
        super(message);
    }

    public DatabaseServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
