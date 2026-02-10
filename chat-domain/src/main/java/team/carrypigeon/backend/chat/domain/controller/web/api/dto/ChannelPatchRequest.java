package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 频道资料更新请求体。
 * <p>
 * 所有字段均为可选，未传字段表示保持原值。
 *
 * @param name 频道名称。
 * @param brief 频道简介。
 * @param avatar 频道头像资源键。
 */
public record ChannelPatchRequest(String name,
                                  String brief,
                                  String avatar) {
}
