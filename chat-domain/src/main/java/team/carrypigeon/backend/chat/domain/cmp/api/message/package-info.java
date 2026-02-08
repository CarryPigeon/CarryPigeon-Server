/**
 * HTTP API 消息相关节点（LiteFlow Node）。
 * <p>
 * 典型链路：
 * <ul>
 *   <li>{@code POST /api/channels/{cid}/messages}：绑定请求 -> 成员校验 -> 限制/校验 -> 构建/保存 -> 返回结果</li>
 *   <li>{@code GET /api/channels/{cid}/messages}：绑定请求 -> 成员校验 -> 查询 -> 组装响应</li>
 * </ul>
 */
package team.carrypigeon.backend.chat.domain.cmp.api.message;

