package team.carrypigeon.backend.chat.domain.features.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.DefaultLifecycleProcessor;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.CustomMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.SystemMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceMessageTypePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.config.BasicInfrastructureAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.id.IdAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.plugin.PluginAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.time.TimeAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RealtimeServerConfiguration 装配契约测试。
 * 职责：验证 server feature 是否按配置装配实时通道运行时相关 Bean。
 * 边界：不实际绑定端口，也不验证发布器工厂内部字段注入细节。
 */
@Tag("contract")
class RealtimeServerConfigurationContextTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BasicInfrastructureAutoConfiguration.class,
                    JacksonAutoConfiguration.class,
                    IdAutoConfiguration.class,
                    PluginAutoConfiguration.class,
                    TimeAutoConfiguration.class,
                    RealtimeServerConfiguration.class
            ))
            .withUserConfiguration(
                    TestSupportConfiguration.class,
                    MessagePluginConfiguration.class,
                    TextMessageTypePluginConfiguration.class,
                    CustomMessageTypePluginConfiguration.class,
                    SystemMessageTypePluginConfiguration.class,
                    FileMessageTypePluginConfiguration.class,
                    VoiceMessageTypePluginConfiguration.class
            );

    /**
     * 实时服务装配测试支撑配置。
     * 职责：提供配置测试所需的时间、JSON、鉴权、用户资料和生命周期处理器替身。
     */
    @Configuration
    static class TestSupportConfiguration {

        @Bean
        TimeProvider timeProvider(Clock clock) {
            return new TimeProvider(clock);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean(name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME)
        LifecycleProcessor lifecycleProcessor() {
            return new DefaultLifecycleProcessor() {
                @Override
                public void onRefresh() {
                    // Context runner tests only verify bean registration and property wiring.
                }
            };
        }

        @Bean
        AuthTokenService authTokenService() {
            return new AuthTokenService() {
                @Override
                public String issueAccessToken(AuthAccount account, java.time.Instant expiresAt) {
                    return "access-token";
                }

                @Override
                public String issueRefreshToken(AuthAccount account, long refreshSessionId, java.time.Instant expiresAt) {
                    return "refresh-token";
                }

                @Override
                public AuthTokenClaims parseAccessToken(String accessToken) {
                    return new AuthTokenClaims("1001", "carry-user", "access", 0L, java.time.Instant.parse("2026-04-20T12:30:00Z"));
                }

                @Override
                public AuthTokenClaims parseRefreshToken(String refreshToken) {
                    return new AuthTokenClaims("1001", "carry-user", "refresh", 2001L, java.time.Instant.parse("2026-05-04T12:00:00Z"));
                }
            };
        }

        @Bean
        ServerIdentityProperties serverIdentityProperties() {
            return new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000");
        }

        @Bean
        NotificationPreferenceRepository notificationPreferenceRepository() {
            return new NotificationPreferenceRepository() {
                @Override
                public java.util.Optional<NotificationServerPreference> findServerPreferenceByAccountId(long accountId) {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.List<NotificationChannelPreference> listChannelPreferencesByAccountId(long accountId) {
                    return java.util.List.of();
                }

                @Override
                public NotificationServerPreference upsertServerPreference(NotificationServerPreference preference) {
                    return preference;
                }

                @Override
                public NotificationChannelPreference upsertChannelPreference(NotificationChannelPreference preference) {
                    return preference;
                }
            };
        }

        @Bean
        UserProfileRepository userProfileRepository() {
            return new UserProfileRepository() {
                @Override
                public java.util.Optional<UserProfile> findByAccountId(long accountId) {
                    return java.util.Optional.of(new UserProfile(accountId, "carry-user", "avatars/u/1001.png", "", 0L, 0L, java.time.Instant.parse("2026-04-20T12:00:00Z"), java.time.Instant.parse("2026-04-20T12:00:00Z")));
                }

                @Override
                public java.util.List<UserProfile> findAll() {
                    return java.util.List.of();
                }

                @Override
                public java.util.List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
                    return java.util.List.of();
                }

                @Override
                public java.util.List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
                    return java.util.List.of();
                }

                @Override
                public UserProfile save(UserProfile userProfile) {
                    return userProfile;
                }

                @Override
                public UserProfile update(UserProfile userProfile) {
                    return userProfile;
                }
            };
        }
    }

    /**
     * 验证开启实时通道配置时会注册实时通道运行时相关 Bean 定义。
     */
    @Test
    @DisplayName("configuration enabled registers runtime bean")
    void configuration_enabled_registersRuntimeBean() {
        contextRunner
                .withPropertyValues(
                        "spring.main.lazy-initialization=true",
                        "cp.chat.server.realtime.enabled=true",
                        "cp.chat.server.realtime.host=127.0.0.1",
                        "cp.chat.server.realtime.port=28080",
                        "cp.chat.server.realtime.path=/api/ws",
                        "cp.chat.server.realtime.boss-threads=1",
                        "cp.chat.server.realtime.worker-threads=0",
                        "cp.infrastructure.id.worker-id=1",
                        "cp.infrastructure.id.datacenter-id=1"
                )
                .run(context -> {
                    assertThat(context).hasBean("realtimeServerRuntime");
                    assertThat(context).hasBean("realtimeChannelInitializer");
                    assertThat(context).doesNotHaveBean("realtimeServerRuntimeStarter");
                    assertThat(context.getBean(RealtimeServerProperties.class).enabled()).isTrue();
                    assertThat(context.getBean(RealtimeServerProperties.class).port()).isEqualTo(28080);
                });
    }

    /**
     * 验证关闭实时通道配置时不会变更属性语义，并保持当前装配行为。
     */
    @Test
    @DisplayName("configuration disabled still registers runtime bean")
    void configuration_disabled_stillRegistersRuntimeBean() {
        contextRunner
                .withPropertyValues(
                        "spring.main.lazy-initialization=true",
                        "cp.chat.server.realtime.enabled=false",
                        "cp.chat.server.realtime.host=127.0.0.1",
                        "cp.chat.server.realtime.port=28080",
                        "cp.chat.server.realtime.path=/api/ws",
                        "cp.chat.server.realtime.boss-threads=1",
                        "cp.chat.server.realtime.worker-threads=0",
                        "cp.infrastructure.id.worker-id=1",
                        "cp.infrastructure.id.datacenter-id=1"
                )
                .run(context -> {
                    assertThat(context).hasBean("realtimeServerRuntime");
                    assertThat(context).hasBean("realtimeChannelInitializer");
                    assertThat(context).doesNotHaveBean("realtimeServerRuntimeStarter");
                    assertThat(context.getBean(RealtimeServerProperties.class).enabled()).isFalse();
                });
    }
}
