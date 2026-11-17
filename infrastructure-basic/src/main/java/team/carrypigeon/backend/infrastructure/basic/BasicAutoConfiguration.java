package team.carrypigeon.backend.infrastructure.basic;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandRegistry;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageDispatcher;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckRunner;

/** 固定基础设施的明确装配入口；不扫描可替换的数据库、邮件、缓存或存储实现。 */
@AutoConfiguration
@EnableConfigurationProperties(RealtimeServerProperties.class)
@Import({AfterCommitEventPublisher.class, RealtimeFrameCodec.class, RealtimeCommandRegistry.class,
        CPChannelMessageDispatcher.class, InitializationCheckRunner.class})
public class BasicAutoConfiguration {}
