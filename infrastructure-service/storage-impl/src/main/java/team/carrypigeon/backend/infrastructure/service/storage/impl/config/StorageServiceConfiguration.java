package team.carrypigeon.backend.infrastructure.service.storage.impl.config;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;

/** 配置 MinIO 及其专用 HTTP 客户端，连接和传输超时仅作用于对象存储。 */
@Configuration(proxyBeanMethods = false)
@EnvironmentComponent(profiles = {PROD, DEV})
public class StorageServiceConfiguration {

    /**
     * 创建 MinIO 客户端。
     *
     * @param properties MinIO 存储配置
     * @return MinIO 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(
            MinioStorageProperties properties,
            @Qualifier("minioHttpClient") OkHttpClient minioHttpClient
    ) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .httpClient(minioHttpClient)
                .build();
    }

    /**
     * 创建 MinIO 专用 HTTP 客户端。
     * 边界：超时只影响 storage-impl，不改变其它 HTTP 客户端。
     *
     * @param properties MinIO 存储配置
     * @return 带明确网络超时的 MinIO HTTP 客户端
     */
    @Bean("minioHttpClient")
    @ConditionalOnMissingBean(name = "minioHttpClient")
    public OkHttpClient minioHttpClient(MinioStorageProperties properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.connectTimeout())
                .readTimeout(properties.readTimeout())
                .writeTimeout(properties.writeTimeout())
                .build();
    }
}
