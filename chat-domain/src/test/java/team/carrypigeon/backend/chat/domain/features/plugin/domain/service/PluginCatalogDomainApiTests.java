package team.carrypigeon.backend.chat.domain.features.plugin.domain.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PluginCatalogDomainApi 契约测试。
 * 职责：验证插件目录直接投影插件声明的 domain，不从内部消息类型推断协议值。
 * 边界：不验证 HTTP 序列化和插件消息发送逻辑。
 */
@Tag("contract")
class PluginCatalogDomainApiTests {

    /**
     * 验证内部 type 与对外 domain 不同时，目录仍返回插件声明的 domain。
     */
    @Test
    @DisplayName("list public plugins returns declared canonical domain")
    void listPublicPlugins_distinctTypeAndDomain_returnsDeclaredDomain() {
        ChannelMessagePlugin plugin = new ChannelMessagePlugin() {
            @Override
            public String supportedType() {
                return "widget";
            }

            @Override
            public String supportedDomain() {
                return "Example:Widget";
            }

            @Override
            public CanonicalData validateCanonicalData(
                    ChannelMessageBuildContext context,
                    String domainVersion,
                    Map<String, Object> data
            ) {
                return new CanonicalData(data, "widget");
            }
        };
        ChannelMessagePluginRegistry registry = new ChannelMessagePluginRegistry(List.of(
                new ChannelMessagePluginRegistration(
                        new ChannelMessagePluginDescriptor(
                                "example-widget",
                                "widget",
                                "widget",
                                "Example widget plugin",
                                true,
                                List.of("message.created"),
                                List.of("message:widget:send"),
                                "always_available"
                        ),
                        plugin
                )
        ));

        var result = new PluginCatalogDomainApi(registry).listPublicPlugins();

        assertEquals(1, result.size());
        assertEquals("widget", result.getFirst().messageType());
        assertEquals("Example:Widget", result.getFirst().domain());
    }

    /**
     * 验证 required plugin 配置归一化和缺失项判断均由 plugin feature 管理。
     */
    @Test
    @DisplayName("required plugins are normalized and missing plugins are reported")
    void requiredPluginIds_blankAndDuplicateEntries_normalizesAndReportsMissingPlugins() {
        PluginCatalogDomainApi api = new PluginCatalogDomainApi(
                new ChannelMessagePluginRegistry(List.of()),
                List.of("", " mc-bind ", "mc-bind", "math-formula")
        );

        assertEquals(List.of("mc-bind", "math-formula"), api.requiredPluginIds());
        assertEquals(List.of("math-formula"), api.findMissingRequiredPlugins(List.of("mc-bind")));
    }
}
