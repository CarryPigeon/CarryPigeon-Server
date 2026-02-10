package team.carrypigeon.backend.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文基础启动测试。
 */
@SpringBootTest(properties = {
        "cp.startup.health-check.enabled=false",
        "connection.enabled=false"
})
class ApplicationstarterApplicationTests {

    /**
     * 验证 Spring 上下文可正常加载。
     */
    @Test
    void contextLoads() {
    }
}
