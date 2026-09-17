package team.carrypigeon.backend.starter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分发脚本插件预检契约测试。
 * 职责：防止 Linux 与 PowerShell 分发入口遗漏正式插件预检或回退到日志文本 readiness。
 */
@Tag("contract")
class DistributionPluginPreflightScriptTests {

    /**
     * 验证两个主验证脚本都以 app、lib、plugins 组成 classpath 并调用统一预检命令。
     */
    @Test
    void verifyScripts_distributionClasspath_invokeSharedPluginPreflight() throws IOException {
        Path distributionRoot = locateDistributionRoot();
        String shell = Files.readString(distributionRoot.resolve("src/bin/verify.sh"));
        String powerShell = Files.readString(distributionRoot.resolve("src/bin/verify.ps1"));

        assertTrue(shell.contains("$BASE_DIR/lib/*:$BASE_DIR/plugins/*"));
        assertTrue(shell.contains("team.carrypigeon.backend.starter.PluginPreflightCommand"));
        assertTrue(powerShell.contains("$LibDir, $PluginDir"));
        assertTrue(powerShell.contains("team.carrypigeon.backend.starter.PluginPreflightCommand"));
    }

    /**
     * 验证后台启动脚本调用正式 readiness 端点，并允许部署环境覆盖探测地址。
     */
    @Test
    void backgroundScripts_runtimeReadiness_probeHttpEndpoint() throws IOException {
        Path distributionRoot = locateDistributionRoot();
        String shell = Files.readString(distributionRoot.resolve("src/bin/start-background.sh"));
        String verifyShell = Files.readString(distributionRoot.resolve("src/bin/verify.sh"));
        String powerShell = Files.readString(distributionRoot.resolve("src/bin/start-background.ps1"));

        assertTrue(shell.contains("CP_READINESS_URL"));
        assertTrue(shell.contains("/internal/readiness"));
        assertTrue(shell.contains("curl --fail --silent --show-error --max-time 2"));
        assertTrue(verifyShell.contains("command -v curl"));
        assertTrue(powerShell.contains("CP_READINESS_URL"));
        assertTrue(powerShell.contains("/internal/readiness"));
        assertTrue(powerShell.contains("Invoke-WebRequest"));
        assertTrue(powerShell.contains("StatusCode -eq 204"));
    }

    private Path locateDistributionRoot() {
        return List.of(Path.of("distribution"), Path.of("../distribution")).stream()
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("distribution module directory does not exist"));
    }
}
