package team.carrypigeon.backend.chat.domain.shared.domain.problem;

/**
 * 统一业务问题类型。
 * 职责：为协议层与应用层之间建立稳定的问题分类。
 * 边界：这里只表达当前阶段需要的最小问题类型，不扩展复杂错误码体系。
 */
public enum ProblemType {
    VALIDATION,
    FORBIDDEN,
    NOT_FOUND,
    INTERNAL
}
