package team.carrypigeon.backend.chat.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * chat-domain 模块边界测试。
 * 职责：阻止通用 domain/port、匿名正式 API 实现回归，并限制 feature 间只能依赖稳定 API 契约包。
 * 边界：通过正式源码文本验证包依赖，不替代业务测试。
 */
@Tag("architecture")
class ModuleBoundaryTests {

    private static final Set<String> CROSS_FEATURE_CONTRACT_PACKAGES = Set.of(
            "api", "command", "query", "projection", "draft"
    );
    private static final Pattern ANONYMOUS_API_IMPLEMENTATION = Pattern.compile(
            "\\bnew\\s+(?:[\\w$]+\\.)*[\\w$]*Api\\s*\\([^)]*\\)\\s*\\{",
            Pattern.DOTALL
    );

    @Test
    void sourceTree_featureBoundaries_onlyExposeApiContracts() throws IOException {
        Path featuresRoot = Path.of("src/main/java/team/carrypigeon/backend/chat/domain/features");
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(featuresRoot)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> inspect(path, violations));
        }
        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    private void inspect(Path source, List<String> violations) {
        String normalizedPath = source.toString().replace('\\', '/');
        if (normalizedPath.contains("/domain/port/")) {
            violations.add("domain/port is forbidden: " + normalizedPath);
        }
        try {
            String sourceText = Files.readString(source);
            if (ANONYMOUS_API_IMPLEMENTATION.matcher(sourceText).find()) {
                violations.add("anonymous formal API implementation is forbidden: " + normalizedPath);
            }
            String owner = featureName(normalizedPath);
            for (String line : sourceText.lines().toList()) {
                String prefix = "import team.carrypigeon.backend.chat.domain.features.";
                if (!line.startsWith(prefix)) {
                    continue;
                }
                String imported = line.substring(prefix.length(), line.length() - 1);
                String[] segments = imported.split("\\.");
                if (segments.length < 3 || owner.equals(segments[0])) {
                    continue;
                }
                if (segments.length < 4 || !"domain".equals(segments[1])
                        || !CROSS_FEATURE_CONTRACT_PACKAGES.contains(segments[2])) {
                    violations.add(normalizedPath + " imports forbidden cross-feature type: " + line);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to inspect " + source, exception);
        }
    }

    private String featureName(String normalizedPath) {
        String marker = "/features/";
        int start = normalizedPath.indexOf(marker) + marker.length();
        return normalizedPath.substring(start, normalizedPath.indexOf('/', start));
    }
}
