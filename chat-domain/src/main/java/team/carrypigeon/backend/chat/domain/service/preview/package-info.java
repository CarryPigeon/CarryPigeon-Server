/**
 * API/WS 消息预览（preview）生成模块。
 * <p>
 * 目标：
 * <ul>
 *   <li>为“未安装插件的客户端”提供可读的降级展示</li>
 *   <li>避免泄露非 {@code Core:*} domain 的 payload 全量数据</li>
 *   <li>使 HTTP 响应与 WS 推送的 preview 语义一致</li>
 * </ul>
 */
package team.carrypigeon.backend.chat.domain.service.preview;

