package team.carrypigeon.backend.chat.domain.features.server.domain.service.entrance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerMetaProperties;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.entrance.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.entrance.result.ServerDiscoveryDocument;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 服务基础领域服务。
 * 职责：为协议层提供当前服务入口的最小用例编排与公开源信息组装。
 * 边界：当前阶段只返回协议骨架状态，不承载具体聊天业务逻辑。
 */
@Service
public class ServerEntranceDomainApi implements ServerEntranceApi {

    private final ServerMetaProperties serverMetaProperties;
    private final RealtimeServerProperties realtimeServerProperties;
    private final TimeProviderImpl timeProvider;

    @Autowired
    public ServerEntranceDomainApi(
        ServerMetaProperties serverMetaProperties,
        RealtimeServerProperties realtimeServerProperties,
        TimeProviderImpl timeProvider
    ) {
        this.serverMetaProperties = serverMetaProperties;
        this.realtimeServerProperties = realtimeServerProperties;
        this.timeProvider = timeProvider;
    }

    @Override
    public ServerDiscoveryDocument getServerDiscoveryDocument() {
        return new ServerDiscoveryDocument(
                serverMetaProperties.getId(),
                serverMetaProperties.getName(),
                serverMetaProperties.getDescription(),
                realtimeServerProperties.wsUrl(),
                timeProvider.nowMillis()
        );
    }

}
