package team.carrypigeon.backend.starter.support.http;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.starter.bootstrap.plugin.PluginReadinessGate;

/**
 * 宿主运行时 readiness 入口。
 * 职责：将启动期插件门禁状态转换为供部署脚本探测的无正文 HTTP 状态。
 * 边界：不公开插件、数据库或其它外部依赖明细，也不表达持续存活状态。
 */
@Hidden
@RestController
public final class RuntimeReadinessController {

    private final PluginReadinessGate readinessGate;

    public RuntimeReadinessController(PluginReadinessGate readinessGate) {
        this.readinessGate = readinessGate;
    }

    /**
     * 返回当前宿主是否已完成启动期插件初始化。
     *
     * @return 已就绪时为 204，未就绪时为 503，均无响应正文
     */
    @GetMapping("/internal/readiness")
    public ResponseEntity<Void> readiness() {
        HttpStatus status = readinessGate.isReady()
                ? HttpStatus.NO_CONTENT
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).build();
    }
}
