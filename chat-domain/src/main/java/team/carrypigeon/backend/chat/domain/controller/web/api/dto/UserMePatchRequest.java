package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 当前用户资料更新请求体。
 * <p>
 * 所有字段均为可选，未传字段表示不更新。
 *
 * @param nickname 新昵称。
 * @param avatar 新头像分享键。
 */
public record UserMePatchRequest(String nickname, String avatar) {
}
