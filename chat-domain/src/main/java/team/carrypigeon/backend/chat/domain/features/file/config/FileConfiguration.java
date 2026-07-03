package team.carrypigeon.backend.chat.domain.features.file.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.file.domain.port.FileAttachmentAccessAuthorizer;
import team.carrypigeon.backend.chat.domain.features.file.support.ChannelMemberFileAttachmentAccessAuthorizer;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileUploadShareKeyCodec;

/**
 * 文件功能配置入口。
 * 职责：装配 file feature 内部编解码与访问授权协作对象。
 * 边界：只负责 Bean 装配，不承载文件上传、下载或频道治理规则。
 */
@Configuration
public class FileConfiguration {

    /**
     * 创建上传 share_key 编解码器。
     * 原因：当前阶段复用 auth JWT secret 作为上传 share_key 签名密钥，避免新增配置项。
     *
     * @param authJwtProperties auth JWT 配置
     * @return 上传 share_key 编解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public FileUploadShareKeyCodec fileUploadShareKeyCodec(AuthJwtProperties authJwtProperties) {
        return new FileUploadShareKeyCodec(authJwtProperties.secret());
    }

    /**
     * 创建默认附件访问授权端口实现。
     *
     * @param channelMemberRepository 频道成员仓储抽象
     * @return 附件访问授权端口
     */
    @Bean
    @ConditionalOnMissingBean
    public FileAttachmentAccessAuthorizer fileAttachmentAccessAuthorizer(
            ChannelMemberRepository channelMemberRepository
    ) {
        return new ChannelMemberFileAttachmentAccessAuthorizer(channelMemberRepository);
    }
}
