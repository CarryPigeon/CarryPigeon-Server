package team.carrypigeon.backend.chat.domain.features.message.support.attachment;

import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息附件 objectKey 规则策略。
 * 职责：统一 message feature 内附件对象键的构建、文件名归一化与发送者范围校验前缀规则。
 * 边界：只封装本地附件 key 规则，不承载对象存储访问与跨 feature 抽象。
 */
public class MessageAttachmentObjectKeyPolicy {

    /**
     * 构建上传阶段使用的 canonical objectKey。
     *
     * @param channelId 频道 ID
     * @param messageType 消息类型
     * @param accountId 账户 ID
     * @param objectId 对象唯一 ID
     * @param filename 原始文件名
     * @return canonical objectKey
     */
    public String buildObjectKey(long channelId, String messageType, long accountId, long objectId, String filename) {
        return "channels/" + channelId
                + "/messages/" + messageType
                + "/accounts/" + accountId
                + "/" + objectId + "-" + sanitizeFilename(filename);
    }

    /**
     * 归一化上传文件名。
     *
     * @param filename 原始文件名
     * @return 去除路径后的稳定文件名
     */
    public String normalizeFilename(String filename) {
        String candidate = filename.trim();
        int slashIndex = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
        if (slashIndex >= 0) {
            candidate = candidate.substring(slashIndex + 1);
        }
        if (candidate.isBlank()) {
            throw ProblemException.validationFailed("filename must not be blank");
        }
        return candidate;
    }

    /**
     * 判断 objectKey 是否仍在当前发送者允许的附件范围内。
     *
     * @param channelId 频道 ID
     * @param messageType 消息类型
     * @param accountId 发送者账户 ID
     * @param objectKey 待校验对象键
     * @return true 表示符合当前发送者范围
     */
    public boolean isWithinSenderScope(long channelId, String messageType, long accountId, String objectKey) {
        return objectKey != null && objectKey.startsWith(senderScopePrefix(channelId, messageType, accountId));
    }

    private String senderScopePrefix(long channelId, String messageType, long accountId) {
        return "channels/" + channelId + "/messages/" + messageType + "/accounts/" + accountId + "/";
    }

    private String sanitizeFilename(String filename) {
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? "attachment" : sanitized;
    }
}
