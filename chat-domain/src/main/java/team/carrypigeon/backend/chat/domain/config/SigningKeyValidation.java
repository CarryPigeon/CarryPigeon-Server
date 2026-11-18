package team.carrypigeon.backend.chat.domain.config;

import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.file.config.FileShareKeyProperties;

/** 运行装配时校验签名用途隔离；离线配置检查使用相同规则，不连接外部服务。 */
@Component
public class SigningKeyValidation {
    private static final Set<String> DEVELOPMENT_KEYS = Set.of(
            "carrypigeon-local-dev-jwt-secret-change-me-32",
            "carrypigeon-local-file-share-key-secret-32");

    /** 两种用途始终使用独立密钥；公开示例仅允许在显式启用 dev 时使用，异常不携带密钥值。 */
    public SigningKeyValidation(Environment environment, AuthJwtProperties jwt, FileShareKeyProperties file) {
        if (jwt.secret().equals(file.secret())) {
            throw new IllegalArgumentException("cp.chat.auth.jwt.secret and cp.chat.file.share-key.secret must differ");
        }
        if (!environment.acceptsProfiles(Profiles.of("dev"))
                && (DEVELOPMENT_KEYS.contains(jwt.secret()) || DEVELOPMENT_KEYS.contains(file.secret()))) {
            throw new IllegalArgumentException("development signing keys require the dev profile; configure independent secrets");
        }
    }
}
