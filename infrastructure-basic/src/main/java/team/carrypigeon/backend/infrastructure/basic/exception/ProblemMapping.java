package team.carrypigeon.backend.infrastructure.basic.exception;

/**
 * 项目异常的对外表现契约，所有异常抛出都需要以该接口的实现类进行包装。
 * 职责：由调用方实现，一次性声明一个失败语义对应的响应码、对外原因与默认消息。
 * 边界：只描述对外表现，不描述触发条件；本接口不感知任何具体实现，新增错误码不需要修改这里。
 */
public interface ProblemMapping {

    /** 稳定的五位业务错误码。 */
    default int code() {
        return 10012;
    }

    /**
     * 失败类别的标准 HTTP 状态，用于下载及日志分级。
     * 普通业务 JSON 由协议入口将此类别映射为 HTTP 200。
     */
    int status();

    /**
     * 对外稳定原因。
     * 约束：必须客户端安全，且同一原因在不同协议通道上保持一致。
     */
    String reason();

    /**
     * 对外默认消息。
     * 约束：必须客户端安全；5xx 场景下必须是内部细节无关的固定文案。
     */
    String message();
}
