package team.carrypigeon.backend.chat.domain.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test: keep API error construction enum-based.
 * <p>
 * Disallow legacy string-reason style in main sources:
 * <ul>
 *   <li>{@code CPProblem.of(int, "reason", ...)}</li>
 *   <li>{@code forbidden("reason", ...)}</li>
 * </ul>
 */
class ProblemReasonContractTests {

    private static final Pattern LEGACY_PROBLEM_WITH_STATUS_AND_STRING_REASON = Pattern.compile(
            "CPProblem\\.of\\s*\\(\\s*(?:\\d+|[A-Za-z_][A-Za-z0-9_]*)\\s*,\\s*\"[a-z_]+\"");

    private static final Pattern LEGACY_FORBIDDEN_WITH_STRING_REASON = Pattern.compile(
            "forbidden\\s*\\(\\s*\"[a-z_]+\"");

    @Test
    void mainSources_useEnumReasonExpected_noLegacyReasonPattern() throws IOException {
        List<String> violations = new ArrayList<>();

        scanSourceTree(Paths.get("src/main/java"), violations);
        scanSourceTree(Paths.get("../api/src/main/java"), violations);

        assertTrue(violations.isEmpty(), () -> "Detected legacy reason usage:\n" + String.join("\n", violations));
    }

    /**
     * 扫描源码目录并收集遗留错误模型用法。
     *
     * @param root 待扫描的源码根目录
     * @param violations 违规项收集列表
     * @throws IOException 执行过程中抛出的异常
     */
    private static void scanSourceTree(Path root, List<String> violations) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        for (Path path : (Iterable<Path>) Files.walk(root)::iterator) {
            if (!Files.isRegularFile(path) || !path.toString().endsWith(".java")) {
                continue;
            }
            String text = Files.readString(path, StandardCharsets.UTF_8);
            findViolations(path, text, LEGACY_PROBLEM_WITH_STATUS_AND_STRING_REASON, violations,
                    "legacy CPProblem.of(status, \"reason\", ...)");
            findViolations(path, text, LEGACY_FORBIDDEN_WITH_STRING_REASON, violations,
                    "legacy forbidden(\"reason\", ...)");
        }
    }

    private static void findViolations(Path file,
                                       String content,
                                       Pattern pattern,
                                       List<String> out,
                                       String category) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int line = 1;
            for (int i = 0; i < matcher.start(); i++) {
                if (content.charAt(i) == '\n') {
                    line++;
                }
            }
            out.add(file + ":" + line + " -> " + category);
        }
    }
}
