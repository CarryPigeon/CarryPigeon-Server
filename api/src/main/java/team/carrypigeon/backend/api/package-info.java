/**
 * 后端公共 API 核心抽象层。
 * <p>
 * 该模块定义跨业务模块共享的协议与流程基元：
 * <ul>
 *   <li>Flow 基元：{@code CPFlowContext}/{@code CPFlowKeys}/{@code CPKey}</li>
 *   <li>错误模型：{@code CPProblem}/{@code CPProblemReason}/{@code CPProblemException}</li>
 *   <li>节点抽象：{@code CPNodeComponent} 与 Guard/Checker/Save/Delete 等基类</li>
 * </ul>
 * <p>
 * 设计目标：
 * <ul>
 *   <li>把“协议约束”沉淀为可复用代码，而不是散落在文档描述中</li>
 *   <li>让上层业务模块只组合这些基元即可实现稳定行为</li>
 * </ul>
 */
package team.carrypigeon.backend.api;

