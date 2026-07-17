package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

/**
 * 用户名密码登录策略。
 * 职责：向鉴权会话领域服务提供是否允许密码登录的业务开关。
 * 边界：只表达领域策略结果，不暴露配置绑定细节。
 *
 * @param enabled 是否允许用户名密码登录
 */
public record AuthPasswordLoginPolicy(boolean enabled) {
}
