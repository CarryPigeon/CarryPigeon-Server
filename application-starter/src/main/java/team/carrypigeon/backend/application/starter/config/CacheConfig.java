package team.carrypigeon.backend.application.starter.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enable Spring Cache for the whole project.
 * Concrete cache manager (Redis) is auto-configured by Spring Boot
 * because Redis is already on the classpath for CPCacheImpl.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}

