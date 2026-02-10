package team.carrypigeon.backend.chat.domain.controller.web.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRouteChainContractTests {

    private static final Pattern CHAIN_CONST_PATTERN = Pattern.compile(
            "private\\s+static\\s+final\\s+String\\s+CHAIN_[A-Z0-9_]+\\s*=\\s*\"([^\"]+)\"\\s*;");
    private static final Pattern CHAIN_XML_PATTERN = Pattern.compile("<chain\\s+name=\"([^\"]+)\"");

    @Test
    void controllerChainConstant_existsInApiXml_expectedTrue() throws IOException {
        Set<String> controllerChains = parseControllerChains(resolveControllerDir());
        Set<String> xmlChains = parseXmlChains(resolveApiConfigDir());

        assertFalse(controllerChains.isEmpty(), "controller CHAIN 常量为空，契约测试无效");

        Set<String> missingInXml = new LinkedHashSet<>(controllerChains);
        missingInXml.removeAll(xmlChains);

        assertTrue(missingInXml.isEmpty(), () -> "以下 controller chain 未在 api_*.xml 中声明: " + missingInXml);
    }

    @Test
    void apiXmlChain_referencedByControllerConstant_expectedTrue() throws IOException {
        Set<String> controllerChains = parseControllerChains(resolveControllerDir());
        Set<String> xmlChains = parseXmlChains(resolveApiConfigDir());

        Set<String> unusedInController = new LinkedHashSet<>(xmlChains);
        unusedInController.removeAll(controllerChains);

        assertTrue(unusedInController.isEmpty(), () -> "以下 api_*.xml chain 未被 controller 使用: " + unusedInController);
    }

    /**
     * 测试辅助方法。
     *
     * @param controllerDir 测试输入参数
     * @return 测试辅助方法返回结果
     * @throws IOException 执行过程中抛出的异常
     */
    private static Set<String> parseControllerChains(Path controllerDir) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        for (Path file : (Iterable<Path>) Files.walk(controllerDir)::iterator) {
            if (!Files.isRegularFile(file) || !file.toString().endsWith(".java")) {
                continue;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            Matcher m = CHAIN_CONST_PATTERN.matcher(content);
            while (m.find()) {
                result.add(m.group(1));
            }
        }
        return result;
    }

    /**
     * 测试辅助方法。
     *
     * @param configDir 测试输入参数
     * @return 测试辅助方法返回结果
     * @throws IOException 执行过程中抛出的异常
     */
    private static Set<String> parseXmlChains(Path configDir) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        try (var stream = Files.list(configDir)) {
            for (Path xml : (Iterable<Path>) stream
                    .filter(p -> p.getFileName().toString().startsWith("api_"))
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))::iterator) {
                String content = Files.readString(xml, StandardCharsets.UTF_8);
                Matcher m = CHAIN_XML_PATTERN.matcher(content);
                while (m.find()) {
                    result.add(m.group(1));
                }
            }
        }
        return result;
    }

    /**
     * 测试辅助方法。
     *
     * @return 测试辅助方法返回结果
     */
    private static Path resolveControllerDir() {
        return Paths.get("src/main/java/team/carrypigeon/backend/chat/domain/controller/web/api");
    }

    /**
     * 测试辅助方法。
     *
     * @return 测试辅助方法返回结果
     */
    private static Path resolveApiConfigDir() {
        Path candidate1 = Paths.get("../application-starter/src/main/resources/config");
        if (Files.isDirectory(candidate1)) {
            return candidate1;
        }
        Path candidate2 = Paths.get("application-starter/src/main/resources/config");
        if (Files.isDirectory(candidate2)) {
            return candidate2;
        }
        throw new IllegalStateException("Cannot locate application-starter config directory");
    }
}
