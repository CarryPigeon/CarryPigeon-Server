package team.carrypigeon.backend.infrastructure.service.storage.impl.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.minio.MinioClient;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.impl.config.MinioStorageProperties;
import team.carrypigeon.backend.infrastructure.service.storage.impl.minio.MinioObjectStorageService;
import team.carrypigeon.backend.infrastructure.service.storage.impl.minio.MinioStorageHealthService;

/**
 * Real MinIO environment tests for storage-impl.
 * Contract: MinioObjectStorageService can put, read metadata, delete, and create a presigned URL for test-owned objects.
 * Boundary: this test runs only when explicitly enabled and never falls back to a mock MinioClient.
 */
@Tag("env")
@Tag("env-storage")
class MinioObjectStorageServiceEnvTests {

    /**
     * Verifies MinIO object lifecycle behavior against a real bucket.
     */
    @Test
    @DisplayName("real minio object round trip")
    void objectStorageService_realMinio_roundTripAndCleanup() throws Exception {
        EnvMinioSettings settings = EnvMinioSettings.fromEnvironment();
        assumeTrue(settings.enabled(), "set CP_ENV_STORAGE_TEST_ENABLED=true or -Dcp.env.storage.test.enabled=true to run env-storage tests");

        MinioStorageProperties properties = new MinioStorageProperties(
                true,
                settings.endpoint(),
                settings.accessKey(),
                settings.secretKey(),
                settings.bucket()
        );
        MinioClient minioClient = MinioClient.builder()
                .endpoint(settings.endpoint())
                .credentials(settings.accessKey(), settings.secretKey())
                .build();
        MinioStorageHealthService healthService = new MinioStorageHealthService(minioClient, properties);
        assumeTrue(healthService.check().available(), "real MinIO bucket is not available");

        MinioObjectStorageService storageService = new MinioObjectStorageService(minioClient, properties);
        String objectKey = namespacedKey("storage-round-trip", "payload.txt");
        byte[] content = "hello env storage".getBytes(StandardCharsets.UTF_8);

        try {
            StorageObject metadata = storageService.put(new PutObjectCommand(
                    objectKey,
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            ));
            assertEquals(objectKey, metadata.objectKey());
            assertEquals(content.length, metadata.size());

            StorageObject stored = storageService.get(new GetObjectCommand(objectKey)).orElseThrow();
            assertEquals(objectKey, stored.objectKey());
            assertEquals(content.length, stored.size());
            assertTrue(stored.content().isEmpty());

            assertTrue(storageService.createPresignedUrl(
                    new PresignedUrlCommand(objectKey, Duration.ofMinutes(1))).url().toString().contains(objectKey));

            storageService.delete(new DeleteObjectCommand(objectKey));
            assertTrue(storageService.get(new GetObjectCommand(objectKey)).isEmpty());
        } finally {
            storageService.delete(new DeleteObjectCommand(objectKey));
        }
    }

    private static String namespacedKey(String caseName, String suffix) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        return "it/" + timestamp + "/" + sanitize(caseName) + "/" + sanitize(suffix);
    }

    private static String sanitize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    private record EnvMinioSettings(boolean enabled, String endpoint, String accessKey, String secretKey, String bucket) {

        static EnvMinioSettings fromEnvironment() {
            return new EnvMinioSettings(
                    booleanValue("cp.env.storage.test.enabled", "CP_ENV_STORAGE_TEST_ENABLED"),
                    value("cp.env.storage.endpoint", "CP_ENV_STORAGE_ENDPOINT", "http://127.0.0.1:9000"),
                    value("cp.env.storage.access-key", "CP_ENV_STORAGE_ACCESS_KEY", "carrypigeon"),
                    value("cp.env.storage.secret-key", "CP_ENV_STORAGE_SECRET_KEY", "carrypigeon123"),
                    value("cp.env.storage.bucket", "CP_ENV_STORAGE_BUCKET", "carrypigeon")
            );
        }

        private static boolean booleanValue(String propertyName, String envName) {
            return Boolean.parseBoolean(value(propertyName, envName, "false"));
        }

        private static String value(String propertyName, String envName, String defaultValue) {
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
            String envValue = System.getenv(envName);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            return defaultValue;
        }
    }
}
