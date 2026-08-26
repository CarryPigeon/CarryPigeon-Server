package team.carrypigeon.backend.chat.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Maven 模块方向架构测试。
 * 职责：从 reactor POM 的直接依赖验证 starter、domain、basic、service API/impl 与 distribution 方向。
 * 边界：只检查仓库内部 `CarryPigeon` 坐标，不限制各模块拥有的第三方依赖。
 */
@Tag("architecture")
class MavenModuleBoundaryTests {

    /**
     * 验证所有 reactor 模块的内部直接依赖均符合既定模块方向。
     */
    @Test
    void reactorModules_internalDependencies_followAllowedDirections() throws Exception {
        Path repositoryRoot = repositoryRoot();
        List<Path> modulePoms = collectModulePoms(repositoryRoot.resolve("pom.xml"));
        Set<String> reactorArtifacts = new HashSet<>();
        for (Path pom : modulePoms) {
            reactorArtifacts.add(requiredDirectChild(documentElement(pom), "artifactId"));
        }
        List<String> violations = new ArrayList<>();
        for (Path pom : modulePoms) {
            Element project = documentElement(pom);
            String owner = requiredDirectChild(project, "artifactId");
            for (Element dependency : directDependencies(project)) {
                String groupId = directChildText(dependency, "groupId");
                String target = directChildText(dependency, "artifactId");
                if (!"CarryPigeon".equals(groupId) || !reactorArtifacts.contains(target)) {
                    continue;
                }
                if (!isAllowed(owner, target)) {
                    violations.add(owner + " -> " + target + " is forbidden in " + pom);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    private boolean isAllowed(String owner, String target) {
        if ("application-starter".equals(owner)) {
            return "chat-domain".equals(target)
                    || "infrastructure-basic".equals(target)
                    || target.endsWith("-api")
                    || target.endsWith("-impl");
        }
        if ("chat-domain".equals(owner)) {
            return "infrastructure-basic".equals(target) || target.endsWith("-api");
        }
        if ("distribution".equals(owner)) {
            return "application-starter".equals(target);
        }
        if (owner.endsWith("-impl")) {
            String correspondingApi = owner.substring(0, owner.length() - "-impl".length()) + "-api";
            return correspondingApi.equals(target) || "infrastructure-basic".equals(target);
        }
        return false;
    }

    private List<Path> collectModulePoms(Path rootPom) throws Exception {
        List<Path> result = new ArrayList<>();
        collectModulePoms(rootPom.toAbsolutePath().normalize(), result, new HashSet<>());
        return List.copyOf(result);
    }

    private void collectModulePoms(Path pom, List<Path> result, Set<Path> visited) throws Exception {
        if (!visited.add(pom)) {
            return;
        }
        result.add(pom);
        Element project = documentElement(pom);
        Element modules = directChild(project, "modules");
        if (modules == null) {
            return;
        }
        for (Element module : directChildren(modules, "module")) {
            collectModulePoms(
                    pom.getParent().resolve(module.getTextContent().trim()).resolve("pom.xml").normalize(),
                    result,
                    visited
            );
        }
    }

    private List<Element> directDependencies(Element project) {
        Element dependencies = directChild(project, "dependencies");
        return dependencies == null ? List.of() : directChildren(dependencies, "dependency");
    }

    private Element documentElement(Path pom) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
    }

    private String requiredDirectChild(Element parent, String name) {
        String value = directChildText(parent, name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(parent.getTagName() + " must declare " + name);
        }
        return value;
    }

    private String directChildText(Element parent, String name) {
        Element child = directChild(parent, name);
        return child == null ? null : child.getTextContent().trim();
    }

    private Element directChild(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private List<Element> directChildren(Element parent, String name) {
        List<Element> children = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getTagName())) {
                children.add(element);
            }
        }
        return children;
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(current.resolve("chat-domain/pom.xml"))) {
            return current;
        }
        if (Files.exists(current.resolve("pom.xml")) && "chat-domain".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        throw new IllegalStateException("cannot locate repository root from " + current);
    }
}
