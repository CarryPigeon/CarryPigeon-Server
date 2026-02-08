/**
 * HTTP API 的 WebSocket 事件流（/api/ws）支持模块。
 * <p>
 * 职责：
 * <ul>
 *   <li>维护已鉴权会话（uid -> sessions）</li>
 *   <li>发布业务事件为 WS event envelope（message.created/deleted 等）</li>
 *   <li>提供断线恢复（resume）所需的事件窗口存储</li>
 * </ul>
 * <p>
 * 文档：
 * <ul>
 *   <li>{@code doc/api/12-WebSocket事件清单.md}</li>
 *   <li>{@code doc/api/10-HTTP+WebSocket协议.md}</li>
 * </ul>
 */
package team.carrypigeon.backend.chat.domain.service.ws;

