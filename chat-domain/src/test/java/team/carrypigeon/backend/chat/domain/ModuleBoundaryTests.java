package team.carrypigeon.backend.chat.domain;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * chat-domain 模块边界测试。
 * 职责：验证跨 feature 可见面、feature 图无环性和正式领域 API 的唯一实现规则。
 * 边界：使用 JDK Java AST 检查正式源码，不替代业务行为或 Maven 模块依赖测试。
 */
@Tag("architecture")
class ModuleBoundaryTests {

    private static final Path FEATURES_ROOT = Path.of(
            "src/main/java/team/carrypigeon/backend/chat/domain/features"
    );
    private static final String FEATURE_PACKAGE_PREFIX =
            "team.carrypigeon.backend.chat.domain.features.";
    private static final Set<String> CROSS_FEATURE_CONTRACT_PACKAGES = Set.of(
            "api", "command", "query", "projection", "draft", "event"
    );
    private static final Pattern ANONYMOUS_API_IMPLEMENTATION = Pattern.compile(
            "\\bnew\\s+(?:[\\w$]+\\.)*[\\w$]*Api\\s*\\([^)]*\\)\\s*\\{",
            Pattern.DOTALL
    );
    private static Set<String> formalApis;
    private static SourceArchitecture architecture;

    /**
     * 单次解析正式源码，供本测试类中的边界、依赖图和实现唯一性断言共享。
     */
    @BeforeAll
    static void analyzeArchitecture() throws IOException {
        ModuleBoundaryTests tests = new ModuleBoundaryTests();
        formalApis = tests.formalApiNames();
        architecture = tests.analyzeSourceTree(formalApis);
    }

    /**
     * 验证普通 import 与代码内 FQCN 都只能引用目标 feature 的稳定领域契约包。
     */
    @Test
    void sourceTree_crossFeatureReferences_onlyUseDomainContracts() {
        assertTrue(
                architecture.boundaryViolations().isEmpty(),
                () -> String.join(System.lineSeparator(), architecture.boundaryViolations())
        );
    }

    /**
     * 验证从全部跨 feature 类型引用构建的依赖图不存在循环。
     */
    @Test
    void sourceTree_featureDependencyGraph_isAcyclic() {
        List<String> cycles = findCycles(architecture.featureDependencies());

        assertTrue(cycles.isEmpty(), () -> "feature dependency cycles:" + System.lineSeparator()
                + String.join(System.lineSeparator(), cycles));
    }

    /**
     * 验证每个正式 domain API 恰有一个命名实现，且实现类只实现一个正式 API。
     */
    @Test
    void sourceTree_formalDomainApis_haveOneDedicatedImplementation() {
        List<String> violations = new ArrayList<>(architecture.apiImplementationViolations());
        formalApis.stream()
                .sorted()
                .filter(api -> architecture.apiImplementations().getOrDefault(api, Set.of()).size() != 1)
                .forEach(api -> violations.add(api + " must have exactly one formal implementation but found "
                        + architecture.apiImplementations().getOrDefault(api, Set.of())));

        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    private SourceArchitecture analyzeSourceTree(Set<String> formalApis) throws IOException {
        List<String> boundaryViolations = new ArrayList<>();
        Map<String, Set<String>> featureDependencies = new LinkedHashMap<>();
        Map<String, Set<String>> apiImplementations = new LinkedHashMap<>();
        List<String> apiImplementationViolations = new ArrayList<>();
        List<Path> sources;
        try (var files = Files.walk(FEATURES_ROOT)) {
            sources = files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler is required for architecture tests");
        }
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            for (Path source : sources) {
                inspectSource(
                        compiler,
                        fileManager,
                        source,
                        formalApis,
                        boundaryViolations,
                        featureDependencies,
                        apiImplementations,
                        apiImplementationViolations
                );
            }
        }
        return new SourceArchitecture(
                List.copyOf(boundaryViolations),
                immutableSets(featureDependencies),
                immutableSets(apiImplementations),
                List.copyOf(apiImplementationViolations)
        );
    }

    private void inspectSource(
            JavaCompiler compiler,
            StandardJavaFileManager fileManager,
            Path source,
            Set<String> formalApis,
            List<String> boundaryViolations,
            Map<String, Set<String>> featureDependencies,
            Map<String, Set<String>> apiImplementations,
            List<String> apiImplementationViolations
    ) throws IOException {
        String normalizedPath = source.toString().replace('\\', '/');
        String owner = featureName(normalizedPath);
        featureDependencies.computeIfAbsent(owner, ignored -> new LinkedHashSet<>());
        if (normalizedPath.contains("/domain/port/")) {
            boundaryViolations.add("domain/port is forbidden: " + normalizedPath);
        }
        String sourceText = Files.readString(source);
        if (ANONYMOUS_API_IMPLEMENTATION.matcher(sourceText).find()) {
            boundaryViolations.add("anonymous formal API implementation is forbidden: " + normalizedPath);
        }
        JavacTask task = (JavacTask) compiler.getTask(
                null,
                fileManager,
                diagnostic -> boundaryViolations.add(normalizedPath + " cannot be parsed: " + diagnostic.getMessage(null)),
                List.of("-proc:none"),
                null,
                fileManager.getJavaFileObjects(source)
        );
        for (CompilationUnitTree unit : task.parse()) {
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
                    inspectFeatureReference(
                            owner,
                            normalizedPath,
                            tree.toString(),
                            boundaryViolations,
                            featureDependencies
                    );
                    return super.visitMemberSelect(tree, unused);
                }

                @Override
                public Void visitClass(ClassTree tree, Void unused) {
                    inspectApiImplementation(
                            normalizedPath,
                            tree,
                            formalApis,
                            apiImplementations,
                            apiImplementationViolations
                    );
                    return super.visitClass(tree, unused);
                }
            }.scan(unit, null);
        }
    }

    private void inspectFeatureReference(
            String owner,
            String source,
            String reference,
            List<String> violations,
            Map<String, Set<String>> dependencies
    ) {
        if (!reference.startsWith(FEATURE_PACKAGE_PREFIX)) {
            return;
        }
        String[] segments = reference.substring(FEATURE_PACKAGE_PREFIX.length()).split("\\.");
        if (segments.length < 4 || owner.equals(segments[0])) {
            return;
        }
        String target = segments[0];
        dependencies.computeIfAbsent(owner, ignored -> new LinkedHashSet<>()).add(target);
        if (!"domain".equals(segments[1]) || !CROSS_FEATURE_CONTRACT_PACKAGES.contains(segments[2])) {
            violations.add(source + " references forbidden cross-feature type: " + reference);
        }
    }

    private void inspectApiImplementation(
            String source,
            ClassTree tree,
            Set<String> formalApis,
            Map<String, Set<String>> implementations,
            List<String> violations
    ) {
        if (tree.getKind() != Tree.Kind.CLASS || tree.getSimpleName().isEmpty()) {
            return;
        }
        Set<String> implementedFormalApis = new LinkedHashSet<>();
        for (Tree implemented : tree.getImplementsClause()) {
            String simpleName = simpleTypeName(implemented.toString());
            if (formalApis.contains(simpleName)) {
                implementedFormalApis.add(simpleName);
                implementations.computeIfAbsent(simpleName, ignored -> new LinkedHashSet<>())
                        .add(tree.getSimpleName() + " (" + source + ")");
            }
        }
        if (implementedFormalApis.size() > 1) {
            violations.add(source + " implements multiple formal domain APIs: " + implementedFormalApis);
        }
    }

    private Set<String> formalApiNames() throws IOException {
        Set<String> names = new LinkedHashSet<>();
        try (var files = Files.walk(FEATURES_ROOT)) {
            files.filter(path -> path.toString().replace('\\', '/').contains("/domain/api/"))
                    .filter(path -> path.getFileName().toString().endsWith("Api.java"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.java$", ""))
                    .sorted()
                    .forEach(names::add);
        }
        return Set.copyOf(names);
    }

    private List<String> findCycles(Map<String, Set<String>> dependencies) {
        List<String> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> active = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        dependencies.keySet().stream().sorted().forEach(feature -> findCycles(
                feature, dependencies, visited, active, path, cycles
        ));
        return cycles.stream().distinct().toList();
    }

    private void findCycles(
            String feature,
            Map<String, Set<String>> dependencies,
            Set<String> visited,
            Set<String> active,
            Deque<String> path,
            List<String> cycles
    ) {
        if (active.contains(feature)) {
            List<String> currentPath = new ArrayList<>(path);
            int start = currentPath.indexOf(feature);
            cycles.add(String.join(" -> ", currentPath.subList(start, currentPath.size())) + " -> " + feature);
            return;
        }
        if (!visited.add(feature)) {
            return;
        }
        active.add(feature);
        path.addLast(feature);
        dependencies.getOrDefault(feature, Set.of()).stream().sorted().forEach(target -> findCycles(
                target, dependencies, visited, active, path, cycles
        ));
        path.removeLast();
        active.remove(feature);
    }

    private String simpleTypeName(String typeName) {
        int genericStart = typeName.indexOf('<');
        String rawType = genericStart < 0 ? typeName : typeName.substring(0, genericStart);
        int packageEnd = rawType.lastIndexOf('.');
        return packageEnd < 0 ? rawType.trim() : rawType.substring(packageEnd + 1).trim();
    }

    private String featureName(String normalizedPath) {
        String marker = "/features/";
        int start = normalizedPath.indexOf(marker) + marker.length();
        return normalizedPath.substring(start, normalizedPath.indexOf('/', start));
    }

    private Map<String, Set<String>> immutableSets(Map<String, Set<String>> values) {
        Map<String, Set<String>> result = new HashMap<>();
        values.forEach((key, value) -> result.put(key, Set.copyOf(value)));
        return Map.copyOf(result);
    }

    private record SourceArchitecture(
            List<String> boundaryViolations,
            Map<String, Set<String>> featureDependencies,
            Map<String, Set<String>> apiImplementations,
            List<String> apiImplementationViolations
    ) {
    }
}
