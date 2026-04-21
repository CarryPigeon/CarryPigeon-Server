package team.carrypigeon.backend.infrastructure.basic.startup;

import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;

/**
 * 初始化检查失败异常。
 * 职责：表达启动期必需检查失败，阻止应用继续进入可用状态。
 * 边界：只表达启动检查失败语义，不替代具体外部服务异常。
 */
public class InitializationCheckFailureException extends InfrastructureException {

    public InitializationCheckFailureException(String checkName, String message) {
        super("Initialization check failed [" + checkName + "]: " + message);
    }
}
