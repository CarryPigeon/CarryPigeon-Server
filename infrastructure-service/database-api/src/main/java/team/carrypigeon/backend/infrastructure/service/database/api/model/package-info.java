/**
 * database-api 模型契约包。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>表达 database-api 与 database-impl 之间传递的最小数据库服务契约模型。</li>
 *   <li>为 {@code chat-domain} 中的领域仓储薄适配器提供最小数据交换对象。</li>
 * </ul>
 *
 * <p>约束：</p>
 * <ul>
 *   <li>这里的 record/model 应优先表达数据库读写所必需的最小字段集合。</li>
 *   <li>不得继续叠加协议展示语义、前端消费语义或复杂业务决策语义。</li>
 *   <li>若某个模型开始与 {@code chat-domain} 领域模型长期并行演化，应优先审查边界是否漂移。</li>
 * </ul>
 */
package team.carrypigeon.backend.infrastructure.service.database.api.model;
