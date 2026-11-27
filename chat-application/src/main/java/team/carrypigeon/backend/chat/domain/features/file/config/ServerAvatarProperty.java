package team.carrypigeon.backend.chat.domain.features.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 服务端头像配置。
 * 职责：承载服务端头像文件的本地路径。
 * 边界：只影响服务端头像下载入口，不影响通用文件上传或附件。
 *
 * @param path 服务端头像文件绝对路径
 */
@ConfigurationProperties(prefix = "cp.chat.file.server-avatar")
public record ServerAvatarProperty(String path) {

    public ServerAvatarProperty {
        if (path == null || path.isBlank()) {
            path = "config/server-avatar.png";
        }
    }
}
