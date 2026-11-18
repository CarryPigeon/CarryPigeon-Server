package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.auth.account;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.cache.DatabaseCache;

/**
 * MyBatis-Plus 鉴权账户数据库服务。
 * 职责：在 database-impl 中完成鉴权账户的最小查询与写入能力。
 * 边界：只负责数据库记录映射，不承载注册与登录业务规则。
 * 缓存：按主键缓存账户实体，按 username/email 唯一键缓存“唯一键 -&gt; 主键”lookup 指针；
 * 写路径在数据库写入后同时精确失效实体键与新旧唯一键指针，不使用模式删除。
 * 安全边界：账户记录含密码摘要，Redis 必须按敏感存储保护；多实例部署时跨实例失效窗口由 TTL 兜底。
 */
public class AuthAccountDatabaseServiceImpl implements AuthAccountDatabaseService {

    private static final String DOMAIN = "auth-account";
    private static final String LOOKUP_USERNAME = "username";
    private static final String LOOKUP_EMAIL = "email";

    private final AuthAccountMapper authAccountMapper;
    private final DatabaseCache databaseCache;

    /**
     * 创建鉴权账户数据库服务。
     *
     * @param authAccountMapper 鉴权账户 Mapper
     * @param databaseCache 数据库查询缓存门面
     */
    public AuthAccountDatabaseServiceImpl(AuthAccountMapper authAccountMapper, DatabaseCache databaseCache) {
        this.authAccountMapper = authAccountMapper;
        this.databaseCache = databaseCache;
    }

    /**
     * 按用户名查询鉴权账户。
     * 缓存：先查 username lookup 指针，命中后按主键读实体缓存；未命中时一次数据库查询同时回填指针与实体。
     * 唯一键比较遵循数据库大小写不敏感语义；缓存指针与库中值不一致时清理指针并回退数据库。
     */
    @Override
    public Optional<AuthAccountRecord> findByUsername(String username) {
        return findByUnique(LOOKUP_USERNAME, username, AuthAccountRecord::username, () -> queryByUsername(username));
    }

    /**
     * 按邮箱查询鉴权账户。
     * 缓存：语义同用户名 lookup，email 唯一键同样按数据库大小写不敏感语义校验。
     */
    @Override
    public Optional<AuthAccountRecord> findByEmail(String email) {
        return findByUnique(LOOKUP_EMAIL, email, AuthAccountRecord::email, () -> queryByEmail(email));
    }

    /**
     * 按账户 ID 查询鉴权账户。
     */
    @Override
    public Optional<AuthAccountRecord> findById(long accountId) {
        return databaseCache.load(
                DatabaseCache.entityKey(DOMAIN, accountId),
                AuthAccountRecord.class,
                () -> queryById(accountId));
    }

    /**
     * 批量读取鉴权账户。
     * 缓存：按主键批量读实体缓存，只对缺失主键回源数据库。
     */
    @Override
    public List<AuthAccountRecord> findByIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return databaseCache.loadAll(
                DOMAIN, accountIds, AuthAccountRecord.class, AuthAccountRecord::id, this::queryByIds);
    }

    /**
     * 插入新的鉴权账户记录。
     */
    @Override
    @Transactional
    public boolean insert(AuthAccountRecord record) {
        int affectedRows = authAccountMapper.insert(AuthAccountEntity.from(record));
        evictRecord(record.id());
        evictLookups(record);
        return affectedRows == 1;
    }

    /**
     * 批量插入鉴权账户记录。
     */
    @Override
    @Transactional
    public boolean insertAll(List<AuthAccountRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        int affectedRows = authAccountMapper.insertAll(records.stream().map(AuthAccountEntity::from).toList());
        for (AuthAccountRecord record : records) {
            evictRecord(record.id());
            evictLookups(record);
        }
        return affectedRows == records.size();
    }

    /**
     * 按账户 ID 删除记录。
     * 缓存：删除前读取旧记录，删除成功后精确失效实体键与旧唯一键指针。
     */
    @Override
    @Transactional
    public boolean deleteById(long accountId) {
        Optional<AuthAccountRecord> previous = databaseCache.enabled() ? queryById(accountId) : Optional.empty();
        int affectedRows = authAccountMapper.deleteById(accountId);
        evictRecord(accountId);
        previous.ifPresent(this::evictLookups);
        return affectedRows == 1;
    }

    /**
     * 按账户 ID 批量删除记录。
     * 缓存：逐账户 ID 精确删除实体缓存。
     */
    @Override
    @Transactional
    public boolean deleteByIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return true;
        }
        // 删除前读取旧记录，删除成功后逐条精确失效实体与旧唯一键指针。
        List<AuthAccountRecord> previous = databaseCache.enabled() ? queryByIds(accountIds) : List.of();
        int affectedRows = authAccountMapper.deleteByIds(accountIds);
        for (Long accountId : accountIds) {
            evictRecord(accountId);
        }
        for (AuthAccountRecord record : previous) {
            evictLookups(record);
        }
        return affectedRows > 0;
    }

    /**
     * 更新既有鉴权账户记录。
     * 缓存：更新前读取旧记录，写入成功后精确失效新实体、旧/新 username 与 email 指针。
     */
    @Override
    @Transactional
    public boolean update(AuthAccountRecord record) {
        Optional<AuthAccountRecord> previous = databaseCache.enabled() ? queryById(record.id()) : Optional.empty();
        int affectedRows = authAccountMapper.updateById(AuthAccountEntity.from(record));
        evictRecord(record.id());
        previous.ifPresent(this::evictLookups);
        evictLookups(record);
        return affectedRows == 1;
    }

    /**
     * 分页查询鉴权账户记录。
     * 缓存：分页查询键空间无界，不参与缓存，保留给管理后台/离线维护使用。
     */
    @Override
    public PageResult<AuthAccountRecord> page(PageQuery query) {
        IPage<AuthAccountEntity> result = authAccountMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<AuthAccountEntity>()
                        .orderByAsc(AuthAccountEntity::getId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(AuthAccountEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    private Optional<AuthAccountRecord> findByUnique(
            String field,
            String value,
            Function<AuthAccountRecord, String> uniqueValueExtractor,
            Supplier<Optional<AuthAccountRecord>> loader
    ) {
        // 空白唯一键没有查询意义，直接返回空，不构造缓存键、不访问数据库。
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        // lookup 指针键：cp:db:auth-account:lookup:<username|email>:<值>
        String lookupKey = DatabaseCache.lookupKey(DOMAIN, field, value);
        Optional<AuthAccountRecord> account = databaseCache.loadByLookup(
                lookupKey,
                DOMAIN,
                AuthAccountRecord.class,
                AuthAccountRecord::id,
                loader,
                this::queryById
        );
        // 缓存指针与数据库值不一致时（大小写/尾空格差异或指针过期），清理指针并回退数据库权威查询。
        if (account.isPresent() && !sameUniqueValue(value, uniqueValueExtractor.apply(account.get()))) {
            databaseCache.evict(lookupKey);
            return loader.get();
        }
        return account;
    }

    /**
     * 按数据库大小写不敏感语义比较唯一键；用于识别缓存指针与库中记录不一致的过期指针。
     */
    private static boolean sameUniqueValue(String requested, String stored) {
        return stored != null && requested.trim().equalsIgnoreCase(stored.trim());
    }

    private Optional<AuthAccountRecord> queryById(long accountId) {
        return Optional.ofNullable(authAccountMapper.selectById(accountId))
                .map(AuthAccountEntity::toRecord);
    }

    private List<AuthAccountRecord> queryByIds(List<Long> accountIds) {
        return authAccountMapper.selectByIds(accountIds).stream()
                .map(AuthAccountEntity::toRecord)
                .toList();
    }

    private Optional<AuthAccountRecord> queryByUsername(String username) {
        return Optional.ofNullable(authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccountEntity>()
                        .eq(AuthAccountEntity::getUsername, username)))
                .map(AuthAccountEntity::toRecord);
    }

    private Optional<AuthAccountRecord> queryByEmail(String email) {
        return Optional.ofNullable(authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccountEntity>()
                        .eq(AuthAccountEntity::getEmail, email)))
                .map(AuthAccountEntity::toRecord);
    }

    /**
     * 精确删除主键实体缓存键；写路径统一从这里失效，不使用模式删除。
     */
    private void evictRecord(long accountId) {
        databaseCache.evict(DatabaseCache.entityKey(DOMAIN, accountId));
    }

    private void evictLookups(AuthAccountRecord record) {
        evictLookup(LOOKUP_USERNAME, record.username());
        evictLookup(LOOKUP_EMAIL, record.email());
    }

    /**
     * 精确删除唯一键 lookup 指针；旧值与新值都会被调用方分别清理。
     */
    private void evictLookup(String field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        databaseCache.evict(DatabaseCache.lookupKey(DOMAIN, field, value));
    }
}
