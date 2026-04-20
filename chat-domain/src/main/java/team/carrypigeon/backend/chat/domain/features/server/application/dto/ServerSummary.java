package team.carrypigeon.backend.chat.domain.features.server.application.dto;

/**
 * 服务概览 DTO。
 * 职责：向 HTTP 调用方暴露当前基础协议层的最小运行状态。
 * 边界：这里只提供协议骨架状态，不暴露内部实现细节。
 *
 * @param service 服务名
 * @param httpStatus HTTP 协议层状态
 * @param websocketStatus WebSocket 实时通道状态
 */
public record ServerSummary(String service, String httpStatus, String websocketStatus) {
}
