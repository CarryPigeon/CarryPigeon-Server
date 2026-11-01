package team.carrypigeon.backend.infrastructure.basic.startup;

import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;

/**
 * 初始化检查失败异常。
 * 职责：表达启动期必需检查失败，阻止应用继续进入可用状态。
 * 边界：只表达启动检查失败语义，不替代具体外部服务异常。
 */
public class InitializationCheckFailureException extends InfrastructureException {

    public InitializationCheckFailureException(String checkName, String message) {
        this(checkName, message, null);
    }

    /**
     * 创建保留根因的初始化检查失败异常。
     *
     * @param checkName 检查名称
     * @param message 失败说明
     * @param cause 原始异常，可为空
     */
    public InitializationCheckFailureException(String checkName, String message, Throwable cause) {
        super("Initialization check failed [" + checkName + "]: " + message, cause);
    }
}
