/**
 * LiteFlow 运行时上下文与 Key 契约包。
 * <p>
 * 建议阅读顺序：
 * <ol>
 *   <li>{@link team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys}: 统一上下文 Key 命名与类型定义</li>
 *   <li>{@link team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext}: Key 访问、链路缓存、连接信息</li>
 *   <li>{@link team.carrypigeon.backend.api.chat.domain.flow.CPKey}: 强类型 Key 基元</li>
 * </ol>
 * <p>
 * 该包目标是把“上下文传参规则”沉淀为代码约束，避免魔法字符串散落。
 */
package team.carrypigeon.backend.api.chat.domain.flow;
