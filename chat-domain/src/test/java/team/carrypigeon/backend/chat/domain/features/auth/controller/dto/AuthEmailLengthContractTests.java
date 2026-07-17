package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import jakarta.validation.constraints.Size;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UpdateCurrentUserEmailRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 邮箱长度契约测试。
 * 职责：验证协议层邮箱长度上限与初始化 SQL 中账号字段容量保持一致。
 * 边界：只读取源码仓库内 SQL 初始化脚本，不连接真实数据库。
 */
@Tag("contract")
class AuthEmailLengthContractTests {

    private static final int EMAIL_MAX_LENGTH = 320;

    /**
     * 验证所有邮箱入口 DTO 使用相同的最大邮箱长度。
     */
    @Test
    @DisplayName("email request dto size constraints use schema capacity")
    void emailRequestDtoSizeConstraints_useSchemaCapacity() {
        assertEquals(EMAIL_MAX_LENGTH, emailMaxLength(CreateTokenSessionRequest.class));
        assertEquals(EMAIL_MAX_LENGTH, emailMaxLength(SendEmailCodeRequest.class));
        assertEquals(EMAIL_MAX_LENGTH, emailMaxLength(UpdateCurrentUserEmailRequest.class));
    }

    /**
     * 验证初始化 SQL 的账号用户名字段容量能承载协议层允许的邮箱。
     */
    @Test
    @DisplayName("auth sql username column matches email dto capacity")
    void authSqlUsernameColumn_matchesEmailDtoCapacity() throws Exception {
        String singleModuleSql = Files.readString(projectRoot().resolve("docs/sql/01-auth.sql"));
        String allInOneSql = Files.readString(projectRoot().resolve("docs/sql/00-all-in-one.sql"));

        assertTrue(singleModuleSql.contains("username VARCHAR(" + EMAIL_MAX_LENGTH + ") NOT NULL"));
        assertTrue(allInOneSql.contains("username VARCHAR(" + EMAIL_MAX_LENGTH + ") NOT NULL"));
    }

    private static int emailMaxLength(Class<?> requestType) {
        for (RecordComponent component : requestType.getRecordComponents()) {
            if ("email".equals(component.getName())) {
                Size size = component.getAccessor().getAnnotation(Size.class);
                return size.max();
            }
        }
        throw new IllegalStateException("email component is missing: " + requestType.getName());
    }

    private static Path projectRoot() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(userDir.resolve("docs/sql/01-auth.sql"))) {
            return userDir;
        }
        Path parent = userDir.getParent();
        if (parent != null && Files.exists(parent.resolve("docs/sql/01-auth.sql"))) {
            return parent;
        }
        throw new IllegalStateException("project root with docs/sql/01-auth.sql was not found from " + userDir);
    }
}
