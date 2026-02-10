/**
 * 统一错误模型定义包。
 * <p>
 * 本包承载“错误即契约”的核心结构：
 * <ul>
 *   <li>{@link team.carrypigeon.backend.api.chat.domain.error.CPProblemReason}: 机器可读 reason + HTTP 状态码映射</li>
 *   <li>{@link team.carrypigeon.backend.api.chat.domain.error.CPProblem}: 运行期错误对象（status/reason/message/details）</li>
 *   <li>{@link team.carrypigeon.backend.api.chat.domain.error.CPProblemException}: 链路中断异常</li>
 * </ul>
 * <p>
 * 约束：
 * <ul>
 *   <li>新增业务错误时优先补充 {@code CPProblemReason}</li>
 *   <li>禁止在控制层硬编码 reason/status 字符串</li>
 *   <li>HTTP/WS 错误输出都应复用同一 reason 枚举</li>
 * </ul>
 */
package team.carrypigeon.backend.api.chat.domain.error;
