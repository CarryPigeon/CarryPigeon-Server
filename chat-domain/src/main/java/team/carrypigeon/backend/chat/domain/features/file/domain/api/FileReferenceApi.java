package team.carrypigeon.backend.chat.domain.features.file.domain.api;

import java.util.Optional;

/**
 * 文件引用 API。
 * 职责：向其他 feature 暴露稳定 share key 与对象键转换能力。
 * 边界：不暴露 file 内部编解码器、对象存储访问或下载授权实现。
 * 输入：文件对象键或 share key。
 * 输出：稳定 share key、附件对象键或下载路径。
 * 失败语义：无法生成有效引用时由领域问题异常表达；非附件 share key 查询返回空。
 * 调用方：message 等 feature 只能依赖本接口，不直接依赖 file support 编解码器。
 */
public interface FileReferenceApi {

    /**
     * 为对象键生成稳定文件 share key。
     *
     * @param objectKey 文件对象键
     * @return 可用于文件下载协议的 share key
     */
    String shareKeyForObjectKey(String objectKey);

    /**
     * 从附件 share key 解析对象键。
     *
     * @param shareKey 文件 share key
     * @return 是有效附件引用时返回对象键
     */
    Optional<String> attachmentObjectKey(String shareKey);

    /**
     * 构造 share key 对应的稳定下载路径。
     *
     * @param shareKey 文件 share key
     * @return 文件下载路径
     */
    String downloadPath(String shareKey);

    /**
     * 归一化消息附件文件名。
     *
     * @param filename 客户端文件名
     * @return 去除路径后的稳定文件名
     */
    String normalizeMessageAttachmentFilename(String filename);

    /**
     * 构造消息附件对象键。
     *
     * @param channelId 频道 ID
     * @param messageType 消息类型
     * @param accountId 发送者账户 ID
     * @param objectId 对象唯一 ID
     * @param filename 已归一化文件名
     * @return canonical 对象键
     */
    String buildMessageAttachmentObjectKey(
            long channelId,
            String messageType,
            long accountId,
            long objectId,
            String filename
    );

    /**
     * 判断对象键是否属于指定消息附件发送范围。
     *
     * @param channelId 频道 ID
     * @param messageType 消息类型
     * @param accountId 发送者账户 ID
     * @param objectKey 对象键
     * @return 位于范围内时为 true
     */
    boolean isMessageAttachmentWithinSenderScope(
            long channelId,
            String messageType,
            long accountId,
            String objectKey
    );
}
