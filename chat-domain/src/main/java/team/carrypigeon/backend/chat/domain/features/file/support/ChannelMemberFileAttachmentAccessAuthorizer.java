package team.carrypigeon.backend.chat.domain.features.file.support;

import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.file.domain.port.FileAttachmentAccessAuthorizer;

/**
 * 基于频道成员关系的文件附件访问授权适配器。
 * 职责：把 file feature 的附件访问判定委托给 channel 成员仓储抽象。
 * 边界：仅做语义适配，不承载文件下载或频道治理规则。
 */
public class ChannelMemberFileAttachmentAccessAuthorizer implements FileAttachmentAccessAuthorizer {

    private final ChannelMemberRepository channelMemberRepository;

    public ChannelMemberFileAttachmentAccessAuthorizer(ChannelMemberRepository channelMemberRepository) {
        this.channelMemberRepository = channelMemberRepository;
    }

    @Override
    public boolean canAccessChannelAttachment(long accountId, long channelId) {
        return channelMemberRepository.exists(channelId, accountId);
    }
}
