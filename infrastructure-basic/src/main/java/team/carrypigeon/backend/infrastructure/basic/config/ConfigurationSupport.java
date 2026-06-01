package team.carrypigeon.backend.infrastructure.basic.config;

import org.springframework.util.StringUtils;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureErrorCode;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;

/**
 * 配置基础工具。
 * 职责：提供最小配置校验能力，避免相同基础校验逻辑散落在各配置类中。
 */
public final class ConfigurationSupport {

    private ConfigurationSupport() {
    }

    /**
     * 校验配置字符串值非空白。
     * 输入：配置值与字段名。
     * 失败语义：为空白时抛出统一的配置绑定基础设施异常。
     *
     * @param value 配置值
     * @param fieldName 配置字段名
     */
    public static void requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.CONFIG_BIND_FAILED,
                    fieldName + " must not be blank"
            );
        }
    }
}
