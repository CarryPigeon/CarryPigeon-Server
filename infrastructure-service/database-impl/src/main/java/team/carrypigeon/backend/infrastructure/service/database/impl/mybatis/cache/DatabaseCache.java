package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.database.impl.config.DatabaseServiceProperties;

/**
 * 数据库查询缓存：自定义 key-value + lookup 指针 + 精确失效。
 *
 * <p>职责：为 database-impl 的查询实现提供缓存读写、回填与失效原语。</p>
 *
 * <p>设计要点：</p>
 * <ol>
 *     <li><b>自定义键值</b>：键统一为 {@code cp:db:<domain>:<entity|lookup>:<part>...}，字符串分段做 URL 编码，
 *     每个业务对象只对应一个 entity 键；所有进入本类的查询默认走缓存，没有按查询配置的开关。</li>
 *     <li><b>lookup 指针</b>：唯一键查询只缓存“唯一键 -&gt; 主键”指针，实体负载只按主键缓存一份，
 *     写路径可同时精确失效指针与实体。</li>
 *     <li><b>精确失效</b>：写路径只删除已知主键/唯一键对应的键，不使用 SCAN 或通配删除。</li>
 *     <li><b>批量读取</b>：按主键批量读缓存，只对缺失主键回源数据库；超过批量上限直接读库。</li>
 *     <li><b>容错降级</b>：缓存读写、删除、反序列化失败只记 WARN 并按未命中/直读处理；缓存后端缺失时全部直读数据库。</li>
 *     <li><b>序列化隔离</b>：缓存使用全局 ObjectMapper 的副本并关闭时间戳序列化，时间统一 ISO-8601，
 *     避免 HTTP 时间戳约定在进程内读写之间造成时间错乱。</li>
 * </ol>
 *
 * <p>空结果不写缓存：查询不到数据时直接返回空，下一次查询会再次回源数据库。这样缓存层只负责“缓存命中数据”，
 * 不引入负缓存状态和额外配置，复杂度更低。</p>
 *
 * <p>一致性边界：采用 cache-aside，先写数据库再删除缓存；多实例之间与并发回填窗口由 TTL 兜底，
 * 因此这里只应缓存允许短窗口最终一致的高频只读数据。</p>
 */
@Slf4j
public final class DatabaseCache {

    /** 缓存域：鉴权账户。 */
    public static final String DOMAIN_AUTH_ACCOUNT = "auth-account";

    /** 缓存域：用户资料。 */
    public static final String DOMAIN_USER_PROFILE = "user-profile";

    /** 缓存域：频道。 */
    public static final String DOMAIN_CHANNEL = "channel";

    /** 缓存域：频道成员关系。 */
    public static final String DOMAIN_CHANNEL_MEMBER = "channel-member";

    /** 缓存域：频道封禁关系。 */
    public static final String DOMAIN_CHANNEL_BAN = "channel-ban";

    /** 所有数据库缓存键的统一前缀，避免与其它业务键冲突。 */
    private static final String KEY_PREFIX = "cp:db";

    /** 主键负载缓存的键类型。 */
    private static final String ENTITY_KIND = "entity";

    /** 唯一键到主键指针缓存的键类型。 */
    private static final String LOOKUP_KIND = "lookup";

    private static final String WILDCARD = "*";
    private static final String WILDCARD_ESCAPED = "%2A";

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final DatabaseServiceProperties properties;

    /**
     * 创建数据库查询缓存门面。
     *
     * @param cacheService 字符串缓存实现；为空表示缓存后端未装配，本类自动退化为直读数据库
     * @param objectMapper 项目统一 ObjectMapper，用于派生缓存专用序列化副本
     * @param properties 数据库服务配置，提供缓存 TTL 与批量上限
     */
    public DatabaseCache(
            CacheService cacheService,
            ObjectMapper objectMapper,
            DatabaseServiceProperties properties
    ) {
        this.cacheService = cacheService;
        // 复制全局 mapper 并关闭时间戳序列化：HTTP 出口使用毫秒时间戳，而进程内缓存需要可逆的 ISO-8601 字符串，
        // 两者混用会导致 Instant 序列化/反序列化口径不一致。
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 缓存能力是否可用。
     *
     * @return 缓存后端存在时为 true
     */
    public boolean enabled() {
        return cacheService != null;
    }

    /**
     * 渲染按主键定位的实体缓存键。
     * 示例：{@code cp:db:user-profile:entity:42}、{@code cp:db:channel-member:entity:3:9}。
     *
     * @param domain 缓存域
     * @param ids 主键分段
     * @return 实体缓存键
     */
    public static String entityKey(String domain, long... ids) {
        StringBuilder builder = new StringBuilder(KEY_PREFIX)
                .append(':').append(domain)
                .append(':').append(ENTITY_KIND);
        // 复合主键按顺序拼接到同一条 entity 键上，保证“一个业务对象对应一个键”。
        for (long id : ids) {
            builder.append(':').append(id);
        }
        return builder.toString();
    }

    /**
     * 渲染唯一键到主键指针的 lookup 缓存键。
     * 示例：{@code cp:db:auth-account:lookup:email:user%40example.com}。
     *
     * @param domain 缓存域
     * @param field 唯一键字段语义（username、email、relation 等稳定常量）
     * @param values 唯一键分段
     * @return lookup 缓存键
     */
    public static String lookupKey(String domain, String field, String... values) {
        StringBuilder builder = new StringBuilder(KEY_PREFIX)
                .append(':').append(domain)
                .append(':').append(LOOKUP_KIND)
                .append(':').append(encode(field));
        // 分段逐个编码后拼接，避免用户输入中的 ':'、'*' 等字符改变键结构。
        for (String value : values) {
            builder.append(':').append(encode(value));
        }
        return builder.toString();
    }

    /**
     * 单实体读取：命中直接返回，未命中执行加载器并回填。
     *
     * @param key 实体缓存键
     * @param type 返回类型
     * @param loader 未命中时的数据库加载器
     * @param <T> 返回类型
     * @return 命中或加载结果；记录不存在时为空
     */
    public <T> Optional<T> load(String key, Class<T> type, Supplier<Optional<T>> loader) {
        // 缓存后端缺失时退化为直读数据库，不影响数据库服务可用性。
        if (!enabled()) {
            return loader.get();
        }
        Optional<T> cached = read(key, type);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<T> loaded = loader.get();
        // 只回填命中的记录；空结果不回填，保持缓存层无负缓存状态。
        loaded.ifPresent(value -> write(key, value, properties.cacheEntityTtl()));
        return loaded;
    }

    /**
     * 批量实体读取：按主键一次批量读缓存，只对缺失主键回源数据库，并回填结果。
     * 语义：返回顺序与入参主键顺序一致（去重后），不存在的主键被跳过。
     * 边界：主键数量超过 {@code cache-max-batch-size} 时直接读库，避免超大结果集写缓存。
     *
     * @param domain 缓存域
     * @param ids 主键列表
     * @param type 返回类型
     * @param idExtractor 从记录中提取主键
     * @param loader 缺失主键的数据库加载器
     * @param <T> 返回类型
     * @return 按入参顺序排列的命中记录
     */
    public <T> List<T> loadAll(
            String domain,
            List<Long> ids,
            Class<T> type,
            ToLongFunction<T> idExtractor,
            Function<List<Long>, List<T>> loader
    ) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        // 去重并保持入参顺序：返回顺序不依赖数据库 ORDER BY，调用方语义稳定。
        List<Long> orderedIds = ids.stream().distinct().toList();
        if (!enabled() || orderedIds.size() > properties.cacheMaxBatchSize()) {
            // 缓存不可用或批量过大：整批回源数据库，不做半缓存。
            return orderById(orderedIds, loader.apply(orderedIds), idExtractor);
        }

        // 1. 一次批量读缓存，只拿到命中的主键值。
        List<String> keys = orderedIds.stream().map(id -> entityKey(domain, id)).toList();
        Map<String, String> rawValues = readAll(keys);
        Map<Long, T> values = new LinkedHashMap<>();
        List<Long> missingIds = new ArrayList<>();
        for (int index = 0; index < orderedIds.size(); index++) {
            long id = orderedIds.get(index);
            String raw = rawValues.get(keys.get(index));
            Optional<T> cached = raw == null ? Optional.empty() : decode(keys.get(index), type, raw);
            if (cached.isPresent()) {
                values.put(id, cached.get());
            } else {
                missingIds.add(id);
            }
        }

        // 2. 只对未命中的主键回源数据库，并逐条回填。
        if (!missingIds.isEmpty()) {
            for (T record : loader.apply(List.copyOf(missingIds))) {
                long id = idExtractor.applyAsLong(record);
                values.put(id, record);
                write(entityKey(domain, id), record, properties.cacheEntityTtl());
            }
        }

        // 3. 按入参顺序组装结果；数据库未返回的主键直接跳过。
        return orderedIds.stream()
                .map(values::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 唯一键 lookup 读取：优先读“唯一键 -&gt; 主键”指针，再按主键读实体负载；冷未命中时一次数据库查询同时回填两者。
     * 指针命中但实体不存在时清理悬空指针，避免已删除记录被旧指针重新带回。
     *
     * @param lookupKey 唯一键 lookup 缓存键
     * @param entityDomain 实体缓存域，用于按主键构造实体键
     * @param type 实体类型
     * @param idExtractor 从记录中提取主键
     * @param lookupLoader 唯一键未命中时的数据库加载器
     * @param idLoader 指针命中但实体未命中时的按主键加载器
     * @param <T> 实体类型
     * @return 查询结果；记录不存在时为空
     */
    public <T> Optional<T> loadByLookup(
            String lookupKey,
            String entityDomain,
            Class<T> type,
            ToLongFunction<T> idExtractor,
            Supplier<Optional<T>> lookupLoader,
            LongFunction<Optional<T>> idLoader
    ) {
        if (!enabled()) {
            return lookupLoader.get();
        }

        // 1. 先看唯一键指针是否命中。
        Optional<Long> pointer = read(lookupKey, Long.class);
        if (pointer.isPresent()) {
            long id = pointer.get();
            String entityKey = entityKey(entityDomain, id);

            // 2. 指针命中后再看实体缓存；实体缺失才按主键回源。
            Optional<T> cachedEntity = read(entityKey, type);
            if (cachedEntity.isPresent()) {
                return cachedEntity;
            }
            Optional<T> loaded = idLoader.apply(id);
            if (loaded.isPresent()) {
                write(entityKey, loaded.get(), properties.cacheEntityTtl());
                return loaded;
            }
            // 指针指向的记录已不存在：清理悬空指针，并用唯一键权威查询兜底，
            // 防止主键删除后唯一键已被重新分配给其他记录时错误返回“不存在”。
            evict(lookupKey);
            return lookupLoader.get();
        }

        // 3. 冷未命中：一次唯一键查询同时回填实体负载与指针，避免再查一次主键。
        Optional<T> loaded = lookupLoader.get();
        // 唯一键不存在时不写负缓存，下一次查询继续回源数据库。
        if (loaded.isEmpty()) {
            return Optional.empty();
        }
        T record = loaded.get();
        long id = idExtractor.applyAsLong(record);
        write(entityKey(entityDomain, id), record, properties.cacheEntityTtl());
        write(lookupKey, id, properties.cacheLookupTtl());
        return loaded;
    }

    /**
     * 精确删除一个缓存键；缓存未启用或后端异常时静默降级。
     *
     * @param key 缓存键
     */
    public void evict(String key) {
        if (!enabled()) {
            return;
        }
        try {
            cacheService.delete(key);
        } catch (RuntimeException ex) {
            log.warn("数据库缓存删除失败：key={}", key, ex);
        }
    }

    /**
     * 读单个键并反序列化。
     * 输出：命中且值合法时返回缓存值；未命中、后端异常、脏值统一返回空，由调用方回源数据库。
     */
    private <T> Optional<T> read(String key, Class<T> type) {
        String raw;
        try {
            Optional<String> cached = cacheService.get(key);
            if (cached.isEmpty()) {
                return Optional.empty();
            }
            raw = cached.get();
        } catch (RuntimeException ex) {
            log.warn("数据库缓存读取失败，回退数据库：key={}", key, ex);
            return Optional.empty();
        }
        return decode(key, type, raw);
    }

    /**
     * 反序列化缓存原始字符串。
     * 失败或得到 null 时删除脏键并按未命中处理，避免坏数据长期占用缓存。
     */
    private <T> Optional<T> decode(String key, Class<T> type, String raw) {
        try {
            T value = fromJson(raw, type);
            if (value != null) {
                return Optional.of(value);
            }
            log.warn("数据库缓存反序列化为空，按未命中处理：key={}", key);
        } catch (RuntimeException ex) {
            log.warn("数据库缓存反序列化失败，按未命中处理：key={}", key, ex);
        }
        evict(key);
        return Optional.empty();
    }

    /**
     * 批量读多个键；后端异常时返回空 Map，让全部主键回源数据库。
     */
    private Map<String, String> readAll(List<String> keys) {
        try {
            return cacheService.getAll(keys);
        } catch (RuntimeException ex) {
            log.warn("数据库缓存批量读取失败，全部回退数据库：keyCount={}", keys.size(), ex);
            return Map.of();
        }
    }

    /**
     * 序列化并写入缓存；写失败只记日志，不影响查询结果。
     */
    private void write(String key, Object value, Duration ttl) {
        try {
            cacheService.set(key, toJson(value), ttl);
        } catch (RuntimeException ex) {
            log.warn("数据库缓存写入失败：key={}", key, ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize database cache value", ex);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to deserialize database cache value", ex);
        }
    }

    /**
     * 把数据库返回的记录按入参主键顺序重排；数据库缺失的主键自然跳过。
     */
    private static <T> List<T> orderById(List<Long> ids, List<T> records, ToLongFunction<T> idExtractor) {
        Map<Long, T> byId = new LinkedHashMap<>();
        for (T record : records) {
            byId.putIfAbsent(idExtractor.applyAsLong(record), record);
        }
        List<T> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            T record = byId.get(id);
            if (record != null) {
                result.add(record);
            }
        }
        return List.copyOf(result);
    }

    /**
     * URL 编码键分段：防止用户输入中的 ':'、'*' 等字符破坏键结构或引入通配语义。
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace(WILDCARD, WILDCARD_ESCAPED);
    }
}
