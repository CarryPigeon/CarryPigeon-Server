package team.carrypigeon.backend.application.starter.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存能力开关配置。
 * <p>
 * 通过 `@EnableCaching` 启用 Spring Cache 注解支持，
 * 具体缓存实现由运行环境自动装配（当前工程默认使用 Redis）。
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
