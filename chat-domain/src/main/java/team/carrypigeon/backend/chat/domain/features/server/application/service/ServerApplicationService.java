package team.carrypigeon.backend.chat.domain.features.server.application.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.CurrentPresenceResult;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.CurrentPresenceStatus;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.WellKnownServerDocument;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;

/**
 * 服务基础应用服务。
 * 职责：为协议层提供当前服务入口的最小用例编排与公开源信息组装。
 * 边界：当前阶段只返回协议骨架状态，不承载具体聊天业务逻辑。
 */
@Service
public class ServerApplicationService {

    private static final List<String> LOGIN_METHODS = List.of("username_password");
    private static final List<String> PUBLIC_CAPABILITIES = List.of("user_registration", "username_password_login");

    private final ServerIdentityProperties serverIdentityProperties;
    private final String applicationName;
    private final ChannelMessagePluginRegistry channelMessagePluginRegistry;
    private final RealtimeServerProperties realtimeServerProperties;
    private final RealtimeSessionRegistry realtimeSessionRegistry;

    public ServerApplicationService(
            ServerIdentityProperties serverIdentityProperties,
            @Value("${spring.application.name:CarryPigeonBackend}") String applicationName,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            RealtimeServerProperties realtimeServerProperties,
            RealtimeSessionRegistry realtimeSessionRegistry
    ) {
        this.serverIdentityProperties = serverIdentityProperties;
        this.applicationName = applicationName;
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
        this.realtimeServerProperties = realtimeServerProperties;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
    }

    /**
     * 返回服务端公开源信息文档。
     *
     * @return 收敛后的最小公开源信息文档
     */
    public WellKnownServerDocument getWellKnownServerDocument() {
        return new WellKnownServerDocument(
                serverIdentityProperties.id(),
                applicationName,
                true,
                LOGIN_METHODS,
                PUBLIC_CAPABILITIES,
                resolvePublicPlugins()
        );
    }

    private List<String> resolvePublicPlugins() {
        return channelMessagePluginRegistry.getPublicPluginKeys();
    }

    /**
     * 返回当前账户在本节点上的 presence 状态。
     *
     * @param accountId 账户 ID
     * @return presence 查询结果
     */
    public CurrentPresenceResult getCurrentPresence(long accountId) {
        if (!realtimeServerProperties.enabled()) {
            return new CurrentPresenceResult(accountId, CurrentPresenceStatus.UNAVAILABLE, 0);
        }
        int onlineSessionCount = realtimeSessionRegistry.getChannels(accountId).size();
        CurrentPresenceStatus status = onlineSessionCount > 0 ? CurrentPresenceStatus.ONLINE : CurrentPresenceStatus.OFFLINE;
        return new CurrentPresenceResult(accountId, status, onlineSessionCount);
    }
}
