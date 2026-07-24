package team.carrypigeon.backend.starter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分发脚本插件预检契约测试。
 * 职责：防止 Linux 与 PowerShell verify 入口遗漏正式 Java 插件预检或错误排除插件目录。
 */
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

    private Path locateDistributionRoot() {
        return List.of(Path.of("distribution"), Path.of("../distribution")).stream()
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("distribution module directory does not exist"));
    }
}
