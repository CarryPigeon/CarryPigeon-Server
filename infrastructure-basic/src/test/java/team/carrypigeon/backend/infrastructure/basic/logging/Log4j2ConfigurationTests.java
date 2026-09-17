package team.carrypigeon.backend.infrastructure.basic.logging;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Log4j2Configuration 契约测试。
 * 职责：验证统一日志配置的性能边界、编码与结构化上下文字段不会回退。
 * 边界：只验证项目配置契约，不重复测试 Log4j2 的文件写入实现。
 */
@Tag("contract")
class Log4j2ConfigurationTests {

    /**
     * 验证文本布局使用 logger name 和 UTF-8，且不启用昂贵的调用者类位置解析。
     */
    @Test
    void textLayouts_unifiedPattern_avoidCallerLocationAndUseUtf8() throws Exception {
        Document configuration = loadConfiguration();
        String sharedPattern = propertyValue(configuration, "TEXT_LOG_PATTERN");

        assertTrue(sharedPattern.contains("%logger"));
        assertTrue(sharedPattern.contains("trace_id=%X{trace_id}"));
        assertFalse(sharedPattern.contains("%C"));

        NodeList layouts = configuration.getElementsByTagName("PatternLayout");
        for (int index = 0; index < layouts.getLength(); index++) {
            Element layout = (Element) layouts.item(index);
            assertEquals("UTF-8", layout.getAttribute("charset"));
            assertFalse(layout.getAttribute("pattern").contains("%C"));
        }
    }

    /**
     * 验证 JSON 错误日志包含 MDC 属性，并关闭额外调用者位置计算。
     */
    @Test
    void jsonLayout_errorAppender_includesContextWithoutLocationLookup() throws Exception {
        Document configuration = loadConfiguration();
        NodeList layouts = configuration.getElementsByTagName("JSONLayout");

        assertEquals(1, layouts.getLength());
        Element layout = (Element) layouts.item(0);
        assertEquals("true", layout.getAttribute("properties"));
        assertEquals("false", layout.getAttribute("locationInfo"));
        assertEquals("UTF-8", layout.getAttribute("charset"));
    }

    /**
     * 验证归档默认保留三十天且可由 JVM 或环境变量覆盖。
     */
    @Test
    void archiveRetention_defaultAndOverrides_useUnifiedProperty() throws Exception {
        Document configuration = loadConfiguration();

        assertEquals(
                "${sys:cp.log.retention:-${env:CP_LOG_RETENTION:-30d}}",
                propertyValue(configuration, "LOG_RETENTION")
        );
    }

    /**
     * 验证过期清理只配置一次、只匹配压缩归档，并保护活动日志。
     */
    @Test
    void archiveCleanup_rolloverAction_deletesOnlyExpiredCompressedLogs() throws Exception {
        Document configuration = loadConfiguration();
        NodeList deleteActions = configuration.getElementsByTagName("Delete");

        assertEquals(1, deleteActions.getLength());
        Element delete = (Element) deleteActions.item(0);
        assertEquals("${LOG_HOME}", delete.getAttribute("basePath"));
        assertEquals("2", delete.getAttribute("maxDepth"));

        NodeList fileConditions = delete.getElementsByTagName("IfFileName");
        assertEquals(1, fileConditions.getLength());
        assertEquals(".*\\.log\\.gz", ((Element) fileConditions.item(0)).getAttribute("regex"));

        NodeList ageConditions = delete.getElementsByTagName("IfLastModified");
        assertEquals(1, ageConditions.getLength());
        assertEquals("${LOG_RETENTION}", ((Element) ageConditions.item(0)).getAttribute("age"));
    }

    private Document loadConfiguration() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/log4j2-spring.xml")) {
            assertNotNull(input);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private String propertyValue(Document configuration, String name) {
        NodeList properties = configuration.getElementsByTagName("Property");
        for (int index = 0; index < properties.getLength(); index++) {
            Element property = (Element) properties.item(index);
            if (name.equals(property.getAttribute("name"))) {
                return property.getTextContent();
            }
        }
        throw new AssertionError("Missing Log4j2 property: " + name);
    }
}
