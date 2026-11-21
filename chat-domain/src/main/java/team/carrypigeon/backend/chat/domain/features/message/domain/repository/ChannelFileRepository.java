package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelFile;

public interface ChannelFileRepository {
    /**
     * 根据分享密钥查找文件
     * @param shareKey 分享密钥
     * @return 文件
     */
    ChannelFile findByShareKey(String shareKey);
}
