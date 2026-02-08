/**
 * HTTP API 的 WebSocket 入口（/api/ws）控制层。
 * <p>
 * 说明：
 * <ul>
 *   <li>该包只负责 WS 协议处理（auth/reauth/subscribe/ping/pong/resume）</li>
 *   <li>具体事件发布由 {@code team.carrypigeon.backend.chat.domain.service.ws} 模块负责</li>
 * </ul>
 */
package team.carrypigeon.backend.chat.domain.controller.web.api.ws;

