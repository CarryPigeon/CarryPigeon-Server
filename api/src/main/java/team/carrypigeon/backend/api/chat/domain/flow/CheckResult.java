package team.carrypigeon.backend.api.chat.domain.flow;

/**
 * 通用的校验结果结构。
 * <p>
 * state 表示校验是否通过；msg 用于承载自定义信息，
 * 在软失败模式下可以由后续节点根据 msg 进行分支处理。
 */
public record CheckResult(boolean state, String msg) {
}

