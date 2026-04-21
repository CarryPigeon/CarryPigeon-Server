package team.carrypigeon.backend.infrastructure.basic.startup;

/**
 * 初始化检查结果。
 * 职责：表达启动检查是否通过，以及供日志与定位使用的稳定说明。
 * 边界：不暴露具体外部服务的技术细节对象。
 *
 * @param passed 检查是否通过
 * @param message 检查结果说明
 */
public record InitializationCheckResult(boolean passed, String message) {

    /**
     * 创建成功结果。
     *
     * @param message 成功说明
     * @return 成功结果
     */
    public static InitializationCheckResult passed(String message) {
        return new InitializationCheckResult(true, message);
    }

    /**
     * 创建失败结果。
     *
     * @param message 失败说明
     * @return 失败结果
     */
    public static InitializationCheckResult failed(String message) {
        return new InitializationCheckResult(false, message);
    }
}
