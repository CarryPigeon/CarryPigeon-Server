package team.carrypigeon.backend.infrastructure.service.database.api;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * database-api 模块边界文档约束测试。
 * 职责：验证 database-api 已存在明确的模块边界说明，避免边界认知继续只停留在口头约定。
 * 边界：只验证文档文件存在与关键约束文本，不验证更广泛的架构语义。
 */
@Tag("contract")
class ModuleBoundaryDocumentationTests {

    /**
     * 验证 database-api 包级边界说明存在且包含禁止复制领域语义的关键约束。
     */
    @Test
    @DisplayName("package info contains database api boundary guidance")
    void packageInfo_containsDatabaseApiBoundaryGuidance() throws Exception {
        Path packageInfo = Path.of("src/main/java/team/carrypigeon/backend/infrastructure/service/database/api/package-info.java");
        String content = Files.readString(packageInfo);

        assertTrue(content.contains("第二套业务模型"));
        assertTrue(content.contains("协议"));
        assertTrue(content.contains("database-api 模块边界说明"));
    }
}
