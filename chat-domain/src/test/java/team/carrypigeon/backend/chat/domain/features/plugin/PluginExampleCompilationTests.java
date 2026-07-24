package team.carrypigeon.backend.chat.domain.features.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 插件示例源码编译测试。
 * 职责：确保 reactor 外的插件示例持续使用当前 plugin feature SPI，不因包迁移或契约重构静默失效。
 * 边界：只编译示例源码，不把示例注册进正式运行时。
 */
@Tag("architecture")
class PluginExampleCompilationTests {

    /**
     * 验证仓库中的消息插件示例可基于当前测试 classpath 完整编译。
     *
     * @param outputDirectory 编译输出临时目录
     * @throws IOException 示例源码读取或编译文件管理失败
     */
    @Test
    void exampleSources_currentPluginContract_compileSuccessfully(@TempDir Path outputDirectory) throws IOException {
        Path exampleRoot = locateExampleRoot();
        List<Path> sourceFiles;
        try (var paths = Files.walk(exampleRoot)) {
            sourceFiles = paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        assertFalse(sourceFiles.isEmpty(), "plugin example source files must exist");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "plugin example compilation requires a JDK");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
            List<String> options = List.of(
                    "-proc:none",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", outputDirectory.toString()
            );
            boolean compiled = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            assertTrue(compiled, () -> diagnostics.getDiagnostics().stream()
                    .map(Object::toString)
                    .reduce((left, right) -> left + System.lineSeparator() + right)
                    .orElse("plugin example compilation failed without diagnostics"));
        }
    }

    private Path locateExampleRoot() {
        List<Path> candidates = List.of(
                Path.of("example/plugin/message-plugin"),
                Path.of("../example/plugin/message-plugin")
        );
        return candidates.stream()
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("plugin example directory does not exist"));
    }
}
