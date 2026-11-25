package team.carrypigeon.backend.infrastructure.service.storage.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.storage.api.exception.StorageServiceException;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/** 测试对象存储，以字节副本保存内容；每次下载独立分配输入流。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryObjectStorage implements ObjectStorageService {
    private record Entry(String contentType, byte[] bytes) {}
    private final Map<String, Entry> objects = new ConcurrentHashMap<>();
    private final Clock clock;

    public MemoryObjectStorage(Clock clock) {
        this.clock = clock;
    }

    @Override
    public StorageObject put(String key, InputStream content, long size, String contentType, String sha256) {
        try {
            byte[] bytes = content.readAllBytes();
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (bytes.length != size || !digest.equalsIgnoreCase(sha256)) {
                throw new IllegalArgumentException("object length or checksum mismatch");
            }
            objects.put(key, new Entry(contentType, bytes));
            return StorageObject.metadata(key, contentType, size);
        } catch (Exception failure) {
            throw new StorageServiceException("failed to put test object", failure);
        }
    }

    @Override
    public Optional<StorageObject> get(String key) {
        return Optional.ofNullable(objects.get(key)).map(entry -> new StorageObject(key, entry.contentType(),
                entry.bytes().length, Optional.of(new ByteArrayInputStream(entry.bytes()))));
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }

    /** 提供可断言的对象 URI 和过期时刻，实际 HTTP 签名下载在 MinIO 环境验证。 */
    @Override
    public PresignedUrl createPresignedUrl(String key, Duration ttl) {
        try {
            return new PresignedUrl(new URI("memory", null, "/" + key, null), clock.instant().plus(ttl));
        } catch (URISyntaxException failure) {
            throw new StorageServiceException("invalid test object key", failure);
        }
    }
}
