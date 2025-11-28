package team.carrypigeon.backend.chat.domain;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试用最小启动配置：
 *  - 扫描 dao 模块，加载真实的 DAO 与数据源配置；
 *  - 扫描 chat-domain 模块，加载所有 LiteFlow Node 与服务。
 */
@SpringBootApplication(
        scanBasePackages = {
                "team.carrypigeon.backend.dao",
                "team.carrypigeon.backend.chat.domain"
        }
)
public class ChatDomainNodeTestConfiguration {
}