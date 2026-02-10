/**
 * 业务责任链节点包（领域实现层）。
 * <p>
 * 该包主要承载：
 * <ul>
 *   <li>Selector/Lister：读取业务实体</li>
 *   <li>Builder/Updater/Setter：构建并修改实体</li>
 *   <li>Saver/Deleter：落库与删除</li>
 * </ul>
 * <p>
 * 与 {@code cmp.api} 分层协作：
 * API 节点负责协议适配，biz 节点负责业务语义。
 */
package team.carrypigeon.backend.chat.domain.cmp.biz;
