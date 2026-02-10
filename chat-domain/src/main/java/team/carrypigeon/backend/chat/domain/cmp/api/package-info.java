/**
 * API 责任链节点包（入站适配层）。
 * <p>
 * 典型节点职责：
 * <ul>
 *   <li>Bind：将请求 DTO 绑定到上下文业务 Key</li>
 *   <li>Guard/Checker：鉴权与参数校验</li>
 *   <li>Result：从上下文组装最终 API 响应</li>
 * </ul>
 * <p>
 * 该层不承载数据库细节，业务查询/写入下沉到 {@code cmp.biz}。
 */
package team.carrypigeon.backend.chat.domain.cmp.api;
