package team.carrypigeon.backend.chat.domain.features.server.application.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.ServerSummary;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.WellKnownServerDocument;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;

/**
 * 服务基础应用服务。
 * 职责：为协议层提供当前服务入口的最小用例编排与公开源信息组装。
 * 边界：当前阶段只返回协议骨架状态，不承载具体聊天业务逻辑。
 */
@Service
public class ServerApplicationService {

    private static final List<String> LOGIN_METHODS = List.of("username_password");

    private final ServerIdentityProperties serverIdentityProperties;
    private final String applicationName;

    public ServerApplicationService(
            ServerIdentityProperties serverIdentityProperties,
            @Value("${spring.application.name:CarryPigeonBackend}") String applicationName
    ) {
        this.serverIdentityProperties = serverIdentityProperties;
        this.applicationName = applicationName;
    }

    /**
     * 返回当前协议骨架的最小服务概览。
     *
     * @return 服务概览数据
     */
    public ServerSummary getSummary() {
        return new ServerSummary("carry-pigeon-backend", "UP", "READY");
    }

    /**
     * 返回服务端公开源信息文档。
     *
     * @return 最小公开源信息文档
     */
    public WellKnownServerDocument getWellKnownServerDocument() {
        return new WellKnownServerDocument(
                serverIdentityProperties.id(),
                applicationName,
                true,
                LOGIN_METHODS
        );
    }
}
