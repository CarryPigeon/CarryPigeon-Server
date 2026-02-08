/**
 * 插件目录与 Domain Contract 相关服务。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>扫描插件包（zip）并生成 {@code /api/plugins/catalog} 与 {@code /api/domains/catalog} 所需的运行时索引</li>
 *   <li>提供非 {@code Core:*} domain 的 payload schema 校验能力（服务端强校验）</li>
 *   <li>不执行插件代码；仅解析 manifest 与 contract schema 文件</li>
 * </ul>
 * <p>
 * 协议/文档：
 * <ul>
 *   <li>{@code doc/PRD.md} 5.3 Domain Contract</li>
 *   <li>{@code doc/api/15-插件包扫描与Manifest规范.md}</li>
 * </ul>
 */
package team.carrypigeon.backend.chat.domain.service.catalog;

