/**
 * database-api 服务契约包。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>定义面向数据库交互能力的最小 service 抽象。</li>
 *   <li>让 {@code chat-domain} 通过稳定服务契约完成数据库读写，而不直接依赖具体实现技术。</li>
 * </ul>
 *
 * <p>约束：</p>
 * <ul>
 *   <li>service 抽象应优先表达数据库能力，而不是复制复杂业务用例。</li>
 *   <li>若某个 service 开始承载明显的业务流程语义、跨协议语义或与数据库无关的场景行为，应重新评估其归属。</li>
 * </ul>
 */
package team.carrypigeon.backend.infrastructure.service.database.api.service;
