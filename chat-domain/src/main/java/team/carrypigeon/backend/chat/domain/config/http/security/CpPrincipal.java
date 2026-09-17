package team.carrypigeon.backend.chat.domain.config.http.security;

/**
 * 用于认证的规则
 * @param accountId 用户数据库中的唯一id
 * 职责：负责定义鉴权的principal
 * */
public record CpPrincipal(Long accountId) {}
