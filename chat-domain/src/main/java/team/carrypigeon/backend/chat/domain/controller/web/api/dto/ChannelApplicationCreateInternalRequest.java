package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 入群申请内部请求。
 * <p>
 * 由控制器组装后传入责任链，统一承载路由参数与请求体。
 *
 * @param cid 频道 ID。
 * @param body 入群申请请求体。
 */
public record ChannelApplicationCreateInternalRequest(String cid, ChannelApplicationCreateRequest body) {
}
