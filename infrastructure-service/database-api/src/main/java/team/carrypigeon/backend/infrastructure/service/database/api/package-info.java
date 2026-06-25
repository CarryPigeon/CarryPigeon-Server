/**
 * database-api 模块边界说明。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>为 {@code chat-domain} 提供数据库服务抽象、事务抽象、健康检查抽象以及必要的数据契约模型。</li>
 *   <li>保证上层不直接依赖具体 JDBC、MyBatis、数据源或数据库驱动实现。</li>
 * </ul>
 *
 * <p>允许承载：</p>
 * <ul>
 *   <li>面向数据库能力的 service 抽象</li>
 *   <li>按能力邻近组织的数据库读写契约 record / model</li>
 *   <li>事务边界抽象</li>
 *   <li>健康检查与数据库服务异常抽象</li>
 * </ul>
 *
 * <p>不允许承载：</p>
 * <ul>
 *   <li>核心业务规则</li>
 *   <li>面向 HTTP / WebSocket / 前端协议的对象</li>
 *   <li>为未来场景预先创建但当前无实现需求的 feature 契约</li>
 *   <li>复制 {@code chat-domain} 领域语义形成第二套业务模型</li>
 * </ul>
 *
 * <p>长期治理要求：</p>
 * <ul>
 *   <li>若新增契约只是表达数据库交互所必需的最小字段与最小行为，可放入本模块。</li>
 *   <li>若新增契约开始表达复杂业务决策、协议展示语义或与数据库技术无关的场景概念，应优先留在 {@code chat-domain}。</li>
 * </ul>
 */
package team.carrypigeon.backend.infrastructure.service.database.api;
