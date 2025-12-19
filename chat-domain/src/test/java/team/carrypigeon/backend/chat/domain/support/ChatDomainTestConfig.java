package team.carrypigeon.backend.chat.domain.support;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal Spring Boot configuration for chat-domain tests.
 * Scans chat-domain components, API controller helpers, and
 * in-memory DAO/cache implementations.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "team.carrypigeon.backend.chat.domain",
                "team.carrypigeon.backend.api.chat.domain.controller",
                "team.carrypigeon.backend.api.starter.server",
                "team.carrypigeon.backend.chat.domain.support.dao"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "team\\.carrypigeon\\.backend\\.chat\\.domain\\.controller\\.web\\..*")
        }
)
public class ChatDomainTestConfig {
}
