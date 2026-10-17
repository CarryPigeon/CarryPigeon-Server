package team.carrypigeon.backend.chat.domain.features.plugin.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 插件 feature 的错误码表。
 * 职责：完整声明插件装载前置条件与消息插件数据契约相关的失败语义。
 * 边界：只服务插件 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum PluginProblem implements ProblemMapping {

    /** 插件数据或插件入参不合法。 */
    INVALID_INPUT(422, "validation_failed", "validation failed"),

    /** 插件引用的文件分享密钥非法。 */
    INVALID_SHARE_KEY(422, "invalid_share_key", "share_key is invalid"),

    /** 插件引用的对象存储对象不存在。 */
    STORAGE_OBJECT_NOT_FOUND(404, "not_found", "storage object does not exist"),

    /** 部署要求的插件未装载，属于前置条件不满足。 */
    REQUIRED_PLUGIN_MISSING(412, "required_plugin_missing", "required plugins are missing"),

    /** 消息携带的插件域版本不受支持。 */
    SCHEMA_VERSION_UNSUPPORTED(422, "schema_invalid", "domain version is not supported"),

    /** 消息携带的插件域没有任何已注册实现。 */
    SCHEMA_DOMAIN_UNSUPPORTED(422, "schema_invalid", "domain is not supported");

    private final int status;
    private final String reason;
    private final String message;

    PluginProblem(int status, String reason, String message) {
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public String message() {
        return message;
    }
}
