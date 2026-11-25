package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.file;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.cache.DatabaseCache;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 文件上传状态持久化；取得写入权和登记对象键原子提交，网络上传不占用数据库事务。
 */
@EnvironmentComponent(profiles = {PROD, DEV})
public class FileDatabaseServiceImpl implements FileDatabaseService {

    private final static String DOMAIN = "file";
    private final FileUploadMapper mapper;
    private final DatabaseCache cache;

    public FileDatabaseServiceImpl(FileUploadMapper mapper, DatabaseCache cache) {
        this.mapper = mapper;
        this.cache = cache;
    }

    @Override
    public boolean insert(FileRecord upload) {
        return mapper.insert(FileEntity.fromFileRecord(upload)) == 1;
    }

    @Override
    public boolean delete(long id) {
        Optional<FileRecord> previous = findById(id);
        if (previous.isEmpty()){
            return false;
        }
        cache.evict(DatabaseCache.entityKey(DOMAIN, previous.get().fileId()));
        cache.evict(DatabaseCache.lookupKey(DOMAIN, "shareKey", previous.get().shareKey()));
        return mapper.deleteById(id) == 1;
    }

    @Override
    public boolean deleteByIds(Collection<Long> ids) {
        List<FileRecord> previous = findByIds(ids);
        previous.forEach(record -> {
            cache.evict(DatabaseCache.entityKey(DOMAIN, record.fileId()));
            cache.evict(DatabaseCache.lookupKey(DOMAIN, "shareKey", record.shareKey()));
        });
        return mapper.deleteByIds(ids) == ids.size();
    }

    @Override
    public Optional<FileRecord> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(FileEntity::toFileRecord);
    }

    @Override
    public List<FileRecord> findByIds(Collection<Long> ids) {
        return mapper.selectByIds(ids).stream().map(FileEntity::toFileRecord).toList();
    }

    @Override
    public Optional<FileRecord> findByShareKey(String shareKey) {
        return cache.loadByLookup(
            DatabaseCache.lookupKey(DOMAIN, "shareKey", shareKey),
            DOMAIN,
            FileRecord.class,
            FileRecord::fileId,
            () -> Optional.ofNullable(
                mapper.selectOne(
                    new LambdaQueryWrapper<FileEntity>()
                        .eq(FileEntity::getShareKey, shareKey)
                )
            ).map(FileEntity::toFileRecord),
            this::findById
        );
    }

    @Override
    public Optional<FileRecord> findBySha256(String sha256) {
        return Optional.ofNullable(
            mapper.selectOne(
                new LambdaQueryWrapper<FileEntity>()
                    .eq(FileEntity::getSha256, sha256)
                    // 同摘要可以有多个上传授权，只有已发布且仍有效的对象可复用。
                    .eq(FileEntity::getStatus, "COMPLETED")
                    .gt(FileEntity::getExpiresAt, Instant.now())
                    .orderByDesc(FileEntity::getExpiresAt, FileEntity::getFileId)
                    .last("LIMIT 1")
            )
        ).map(FileEntity::toFileRecord);
    }

    @Override
    public List<FileRecord> findExpired() {
        LambdaQueryWrapper<FileEntity> query = new LambdaQueryWrapper<FileEntity>()
            .le(FileEntity::getExpiresAt, Instant.now());
        return mapper.selectList(query).stream().map(FileEntity::toFileRecord).toList();
    }

    @Override
    public boolean update(FileRecord record) {
        return mapper.updateById(FileEntity.fromFileRecord(record)) == 1;
    }
}
