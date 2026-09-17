package team.carrypigeon.backend.starter.support.http;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import team.carrypigeon.backend.starter.bootstrap.plugin.PluginReadinessGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 宿主运行时 readiness 协议测试。
 * 职责：验证启动门禁状态被转换为稳定的 204/503 无正文响应。
 */
@Tag("contract")
class RuntimeReadinessControllerTests {

    /**
     * 验证插件初始化尚未完成时返回 503 且不泄露内部信息。
     */
    @Test
    void readiness_gateNotReady_returns503WithoutBody() {
        RuntimeReadinessController controller = new RuntimeReadinessController(new PluginReadinessGate());

        ResponseEntity<Void> response = controller.readiness();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNull(response.getBody());
    }

    /**
     * 验证插件初始化完成后返回 204 且不产生响应正文。
     */
    @Test
    void readiness_gateReady_returns204WithoutBody() {
        PluginReadinessGate gate = new PluginReadinessGate();
        gate.markReady();
        RuntimeReadinessController controller = new RuntimeReadinessController(gate);

        ResponseEntity<Void> response = controller.readiness();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }
}
