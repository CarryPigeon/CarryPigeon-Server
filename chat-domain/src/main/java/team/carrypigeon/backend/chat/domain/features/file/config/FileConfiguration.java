package team.carrypigeon.backend.chat.domain.features.file.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileUploadShareKeyCodec;

/**
 * 文件功能配置入口。
 * 职责：装配 file feature 内部编解码与访问授权协作对象。
 * 边界：只负责 Bean 装配，不承载文件上传、下载或频道治理规则。
 */
@Configuration
@EnableConfigurationProperties(FileShareKeyProperties.class)
public class FileConfiguration {

    /**
     * 创建上传 share_key 编解码器。
     * 原因：上传 share_key 使用 file feature 自有密钥，不与认证令牌签名耦合。
     *
     * @param properties 文件 share key 配置
     * @return 上传 share_key 编解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public FileUploadShareKeyCodec fileUploadShareKeyCodec(FileShareKeyProperties properties) {
        return new FileUploadShareKeyCodec(properties.secret());
    }
}
