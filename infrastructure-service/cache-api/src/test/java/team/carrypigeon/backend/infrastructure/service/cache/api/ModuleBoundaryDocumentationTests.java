package team.carrypigeon.backend.infrastructure.service.cache.api;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * cache-api 模块边界文档约束测试。
 * 职责：验证 cache-api 已存在明确的包级边界说明，避免缓存契约边界继续只停留在口头约定。
 * 边界：只验证文档文件存在与关键约束文本，不验证更广泛的架构语义。
 */
@Tag("contract")
class ModuleBoundaryDocumentationTests {

    /**
     * 验证 cache-api 包级边界说明存在且包含抽象职责与禁止暴露实现细节的关键约束。
     */
    @Test
    @DisplayName("package info contains cache api boundary guidance")
    void packageInfo_containsCacheApiBoundaryGuidance() throws Exception {
        Path packageInfo = Path.of("src/main/java/team/carrypigeon/backend/infrastructure/service/cache/api/package-info.java");
        String content = Files.readString(packageInfo);

        assertTrue(content.contains("cache-api 模块边界说明"));
        assertTrue(content.contains("最小字符串缓存能力抽象"));
        assertTrue(content.contains("不直接依赖 Redis"));
    }
}
