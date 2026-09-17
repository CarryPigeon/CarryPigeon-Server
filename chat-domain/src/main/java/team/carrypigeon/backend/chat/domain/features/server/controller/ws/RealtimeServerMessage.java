package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

/** WebSocket v1 服务端帧 DTO，仅属于协议适配层。 */
record RealtimeServerMessage(String type, String id, Object data, Object error) {
}
