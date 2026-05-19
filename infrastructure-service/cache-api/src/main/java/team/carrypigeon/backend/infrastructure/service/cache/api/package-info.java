/**
 * cache-api 模块边界说明。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>为上层模块提供最小字符串缓存能力抽象、缓存健康检查抽象和必要的缓存契约模型。</li>
 *   <li>保证上层不直接依赖 Redis 客户端、连接实现或序列化细节。</li>
 * </ul>
 *
 * <p>允许承载：</p>
 * <ul>
 *   <li>面向缓存能力的 service 抽象</li>
 *   <li>表达缓存键、缓存条目、TTL 等最小缓存契约的 record / model</li>
 *   <li>缓存健康检查抽象与缓存服务异常抽象</li>
 * </ul>
 *
 * <p>不允许承载：</p>
 * <ul>
 *   <li>Redis、Lettuce、Spring Data Redis 等具体实现细节</li>
 *   <li>核心业务规则</li>
 *   <li>面向 HTTP / WebSocket / 前端协议的对象</li>
 *   <li>为未来缓存场景预先创建但当前无明确实现需求的膨胀契约</li>
 * </ul>
 */
package team.carrypigeon.backend.infrastructure.service.cache.api;
