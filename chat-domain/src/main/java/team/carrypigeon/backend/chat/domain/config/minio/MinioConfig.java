package team.carrypigeon.backend.chat.domain.config.minio;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO Client 配置。
 * <p>
 * 读取配置项：
 * <ul>
 *     <li>{@code minio.endpoint}</li>
 *     <li>{@code minio.accessKey}</li>
 *     <li>{@code minio.secretKey}</li>
 * </ul>
 *
 * <p>安全约束：禁止在日志中输出 accessKey/secretKey。</p>
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    /**
     * 创建 {@link MinioClient}（由 Spring 管理）。
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
