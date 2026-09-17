package team.carrypigeon.backend.infrastructure.basic.exception;

/**
 * TODO 错误码规范化，补充错误解释与范围
 * 基础设施层错误码。
 * 职责：为固定基础设施异常提供稳定、可搜索的错误标识。
 */
public enum InfrastructureErrorCode {
    JSON_SERIALIZE_FAILED,
    JSON_DESERIALIZE_FAILED,
    CONFIG_BIND_FAILED,
    ID_GENERATE_FAILED,
    TIME_ACCESS_FAILED
}
