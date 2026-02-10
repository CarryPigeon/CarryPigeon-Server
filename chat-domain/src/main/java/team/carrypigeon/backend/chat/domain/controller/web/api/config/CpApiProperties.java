package team.carrypigeon.backend.chat.domain.controller.web.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * API 模块配置属性。
 * <p>
 * 对应 `cp.api.*` 配置项，涵盖认证、WS、限流、目录等参数。
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "cp")
public class CpApiProperties {

    /**
     * /api 相关配置。
     */
    @Valid
    private Api api = new Api();
    /**
     * 认证相关配置。
     */
    @Valid
    private Auth auth = new Auth();

    @Data
    public static class Api {
        /**
         * required gate：服务端声明的必需插件列表（缺少则阻止登录）。
         */
        private List<String> requiredPlugins = new ArrayList<>();
        /**
         * 插件目录（Server Catalog）配置。
         * <p>
         * 主要用于：{@code GET /api/plugins/catalog}
         */
        @Valid
        private PluginCatalog pluginCatalog = new PluginCatalog();
        /**
         * Domain 目录配置。
         * <p>
         * 主要用于：{@code GET /api/domains/catalog}
         */
        @Valid
        private DomainCatalog domainCatalog = new DomainCatalog();
        /**
         * 发送消息频率限制配置。
         * <p>
         * 主要用于：{@code POST /api/channels/{cid}/messages}
         */
        @Valid
        private MessageRateLimit messageRateLimit = new MessageRateLimit();
        /**
         * 插件包扫描配置（本地文件系统）。
         */
        @Valid
        private PluginPackageScan pluginPackageScan = new PluginPackageScan();
        /**
         * WebSocket（/api/ws）配置：事件存储、resume 回放等。
         */
        @Valid
        private Ws ws = new Ws();
    }

    @Data
    public static class Auth {
        /**
         * access_token TTL（秒）。
         */
        @Min(1)
        private int accessTokenTtlSeconds = 1800;
        /**
         * refresh_token TTL（天）。
         */
        @Min(1)
        private int refreshTokenTtlDays = 30;
    }

    @Data
    public static class PluginCatalog {
        /**
         * 插件列表（通常由扫描器生成/覆盖）。
         */
        private List<PluginItem> plugins = new ArrayList<>();
    }

    @Data
    public static class PluginItem {
        /**
         * 插件唯一标识（全局唯一）。
         */
        private String pluginId;
        /**
         * 插件展示名称。
         */
        private String name;
        /**
         * 插件版本（SemVer）。
         */
        private String version;
        /**
         * 插件所需的最小宿主版本。
         */
        private String minHostVersion;
        /**
         * 是否为 required 插件（由服务端 requiredPlugins 计算得出）。
         */
        private boolean required;
        /**
         * 权限声明（粗粒度）。
         */
        private List<String> permissions = new ArrayList<>();
        /**
         * 插件提供的 domain 列表（仅保留“具备契约”的项，避免误导客户端）。
         */
        private List<ProvidesDomainItem> providesDomains = new ArrayList<>();
        /**
         * 下载指针与 sha256。
         */
        private Download download;
    }

    @Data
    public static class ProvidesDomainItem {
        /**
         * Domain 名称（例如 Math:Formula）。
         */
        private String domain;
        /**
         * Domain 版本（SemVer）。
         */
        private String domainVersion;
    }

    @Data
    public static class Download {
        /**
         * 下载相对路径（不得包含 host）。
         */
        private String url;
        /**
         * zip 文件 sha256（hex）。
         */
        private String sha256;
    }

    @Data
    public static class DomainCatalog {
        /**
         * Domain 列表（通常由扫描器生成/覆盖）。
         */
        private List<DomainItem> items = new ArrayList<>();
    }

    @Data
    public static class DomainItem {
        /**
         * Domain 名称。
         */
        private String domain;
        /**
         * 服务端支持的版本集合（字符串列表）。
         */
        private List<String> supportedVersions = new ArrayList<>();
        /**
         * 推荐版本（通常为最新稳定版本）。
         */
        private String recommendedVersion;
        /**
         * 服务端强校验约束（大小/深度）。
         */
        private Constraints constraints = new Constraints();
        /**
         * 提供该 domain 的 provider 列表（core/plugin）。
         */
        private List<Provider> providers = new ArrayList<>();
        /**
         * 合约指针（schema_url + sha256）。
         */
        private Contract contract;
    }

    @Data
    public static class Constraints {
        /**
         * 最大 payload 字节数（UTF-8 编码后的 JSON 字符串长度）。
         */
        private Integer maxPayloadBytes;
        /**
         * 最大嵌套深度。
         */
        private Integer maxDepth;
    }

    @Data
    public static class Provider {
        /**
         * provider 类型：core 或 plugin。
         */
        private String type; // core|plugin
        /**
         * plugin provider 的 plugin_id。
         */
        private String pluginId;
        /**
         * plugin provider 的最低插件版本（SemVer）。
         */
        private String minPluginVersion;
    }

    @Data
    public static class Contract {
        /**
         * schema 下载相对路径（不得包含 host）。
         */
        private String schemaUrl;
        /**
         * schema 内容 sha256（hex）。
         */
        private String sha256;
    }

    @Data
    public static class Ws {
        /**
         * 事件存储窗口配置（capacity + ttl）。
         */
        @Valid
        private EventStore eventStore = new EventStore();
        /**
         * 单次 resume 最大回放事件数（防止一次补发过大）。
         */
        @Min(1)
        private int resumeMaxEvents = 2000;
    }

    @Data
    public static class EventStore {
        /**
         * 事件存储容量上限（条数）。
         */
        @Min(1)
        private int capacity = 2000;
        /**
         * 事件存储 TTL（秒）。
         */
        @Min(1)
        private int ttlSeconds = 600;
    }

    @Data
    public static class PluginPackageScan {
        /**
         * 是否启用插件包扫描。
         */
        private boolean enabled = true;
        /**
         * 插件包目录（包含 `*.zip`）。
         * <p>
         * 相对路径将以当前工作目录解析。
         */
        @NotBlank
        private String dir = "plugins/packages";
        /**
         * 是否仅返回每个 plugin_id 的最新版本。
         */
        private boolean latestOnly = true;
        /**
         * 插件包下载端点 base path（相对路径，不含 host），例如：`api/plugins/download`。
         */
        @NotBlank
        private String downloadBasePath = "api/plugins/download";
        /**
         * 合约下载端点 base path（相对路径，不含 host），例如：`api/contracts`。
         */
        @NotBlank
        private String contractBasePath = "api/contracts";

        /**
         * 插件包目录定时扫描刷新间隔（秒）。
         * <p>
         * 设为 0：关闭定时刷新（仍会在启动时扫描一次）。
         */
        @Min(0)
        private int refreshIntervalSeconds = 30;

        /**
         * 插件包信任策略（白名单/签名校验）。
         */
        @Valid
        private PluginTrust trust = new PluginTrust();
    }

    @Data
    public static class PluginTrust {
        /**
         * 是否启用信任策略。
         * <p>
         * 启用后：只有通过 allow/block/sha256/签名校验的插件包才会出现在 catalog 与下载/合约端点中。
         */
        private boolean enabled = false;

        /**
         * plugin_id 白名单：非空时仅允许出现在列表中的 plugin_id（同时仍会应用 blocklist）。
         */
        private List<String> allowedPluginIds = new ArrayList<>();

        /**
         * plugin_id 黑名单：命中即拒绝。
         */
        private List<String> blockedPluginIds = new ArrayList<>();

        /**
         * zip sha256 白名单：非空时仅允许 sha256 命中的插件包。
         */
        private List<String> allowedZipSha256 = new ArrayList<>();

        /**
         * 是否强制要求 Ed25519 签名。
         */
        private boolean requireEd25519Signature = false;

        /**
         * 可信 Ed25519 公钥集合（X.509 SubjectPublicKeyInfo 编码后的 base64）。
         */
        private List<Ed25519PublicKey> ed25519PublicKeys = new ArrayList<>();
    }

    @Data
    public static class Ed25519PublicKey {
        /**
         * 公钥 id（用于匹配 manifest.signing_key_id）。
         */
        private String keyId;
        /**
         * X.509 SPKI 编码后的 base64 公钥。
         */
        private String publicKeyBase64;
    }

    @Data
    public static class MessageRateLimit {
        /**
         * 是否启用发送消息限流。
         */
        private boolean enabled = true;

        /**
         * {@code Core:Text} 的限流窗口（维度：uid+cid）。
         */
        @Valid
        private RateLimitWindow coreText = new RateLimitWindow();

        /**
         * 非 {@code Core:*} domain 的限流窗口（维度：uid+cid+domain）。
         */
        @Valid
        private RateLimitWindow plugin = new RateLimitWindow(10, 10);
    }

    @Data
    public static class RateLimitWindow {
        /**
         * 固定窗口大小（秒）。
         */
        @Min(1)
        private int windowSeconds = 10;
        /**
         * 窗口内最大请求数。
         */
        @Min(1)
        private int maxRequests = 20;
        /**
         * 使用默认参数创建限流窗口。
         */
        public RateLimitWindow() {
        }

        /**
         * 使用指定参数创建限流窗口。
         *
         *  windowSeconds 固定窗口大小（秒）。
         *  maxRequests 窗口内最大请求数。
         */
        public RateLimitWindow(int windowSeconds, int maxRequests) {
            this.windowSeconds = windowSeconds;
            this.maxRequests = maxRequests;
        }
    }
}
