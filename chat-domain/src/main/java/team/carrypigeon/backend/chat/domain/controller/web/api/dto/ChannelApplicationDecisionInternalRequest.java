package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 入群申请审批内部请求。
 *
 * @param cid 频道 ID。
 * @param applicationId 申请 ID。
 * @param body 审批请求体。
 */
public record ChannelApplicationDecisionInternalRequest(String cid,
                                                        String applicationId,
                                                        ChannelApplicationDecisionRequest body) {
}
