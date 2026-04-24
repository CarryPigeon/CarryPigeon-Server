package team.carrypigeon.backend.chat.domain.features.server.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.WellKnownServerDocument;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ServerApplicationService 契约测试。
 * 职责：验证服务端基础应用服务对公开源信息文档的最小组装逻辑。
 * 边界：不验证 HTTP 协议层，只验证应用层输出字段。
 */
@Tag("contract")
class ServerApplicationServiceTests {

    /**
     * 验证公开源信息文档会使用当前 serverId、应用名和最小登录方式集合。
     */
    @Test
    @DisplayName("get well known server document returns minimal public metadata")
    void getWellKnownServerDocument_returnsMinimalPublicMetadata() {
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("carrypigeon-local"),
                "CarryPigeonBackend"
        );

        WellKnownServerDocument result = service.getWellKnownServerDocument();

        assertEquals("carrypigeon-local", result.serverId());
        assertEquals("CarryPigeonBackend", result.serverName());
        assertTrue(result.registerEnabled());
        assertEquals(java.util.List.of("username_password"), result.loginMethods());
    }
}
