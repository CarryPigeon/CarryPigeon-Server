package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.io.InputStream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageAttachmentApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.UploadMessageAttachmentCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 频道消息附件领域 API 实现。
 * 职责：校验成员上下文与附件规格，完成附件类型规范化、对象 key 生成、对象存储写入和上传结果组装。
 * 边界：不读取频道仓储、不创建消息记录、不负责普通文件传输。
 */
@Service
public class ChannelMessageAttachmentDomainApi implements ChannelMessageAttachmentApi {

    private static final String FILE_MESSAGE_TYPE = "file";
    private static final String VOICE_MESSAGE_TYPE = "voice";
    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_VOICE_SIZE_BYTES = 20L * 1024 * 1024;

    private final ChannelContextApi channelContextApi;
    private final FileReferenceApi fileReferenceApi;
    private final IdGenerator idGenerator;
    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;

    public ChannelMessageAttachmentDomainApi(
            ChannelContextApi channelContextApi,
            FileReferenceApi fileReferenceApi,
            IdGenerator idGenerator,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.channelContextApi = channelContextApi;
        this.fileReferenceApi = fileReferenceApi;
        this.idGenerator = idGenerator;
        this.objectStorageServiceProvider = objectStorageServiceProvider;
    }

    @Override
    public MessageAttachmentUploadResult uploadMessageAttachment(UploadMessageAttachmentCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        ChannelContext channel = channelContextApi.requireMemberContext(
                command.channelId(),
                command.accountId()
        );
        if (channel.isMuted()) {
            throw ProblemException.of(MessageProblem.USER_MUTED);
        }
        return uploadAttachment(
                command.accountId(),
                channel.channelId(),
                command.messageType(),
                command.filename(),
                command.mimeType(),
                command.size(),
                command.content()
        );
    }

    /**
     * 上传频道消息附件并生成可被消息 payload 引用的结果。
     * 输入：上传账号、目标频道、附件消息类型、文件元信息和文件内容流。
     * 输出：对象存储 key、share_key、规范化文件名、内容类型和大小。
     * 副作用：向对象存储写入附件内容；对象存储不可用或输入非法时抛出领域问题异常。
     *
     * @param accountId 上传账号 ID
     * @param channelId 附件所属频道 ID
     * @param messageType 附件消息类型，仅支持 file 或 voice
     * @param filename 客户端提交的文件名
     * @param contentType 客户端提交的内容类型，可为空并按消息类型补默认值
     * @param size 文件内容长度
     * @param content 文件内容输入流
     * @return 附件上传结果
     */
    private MessageAttachmentUploadResult uploadAttachment(
            long accountId,
            long channelId,
            String messageType,
            String filename,
            String contentType,
            long size,
            InputStream content
    ) {
        if (content == null) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "file content must not be null");
        }
        if (size <= 0L) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "size must be greater than 0");
        }
        String normalizedMessageType = normalizeAttachmentMessageType(messageType);
        long maxSize = VOICE_MESSAGE_TYPE.equals(normalizedMessageType) ? MAX_VOICE_SIZE_BYTES : MAX_FILE_SIZE_BYTES;
        if (size > maxSize) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "size must be less than or equal to " + maxSize);
        }
        String normalizedFilename = fileReferenceApi.normalizeMessageAttachmentFilename(filename);
        String resolvedContentType = resolveContentType(normalizedMessageType, contentType);
        String objectKey = fileReferenceApi.buildMessageAttachmentObjectKey(
                channelId,
                normalizedMessageType,
                accountId,
                idGenerator.nextLongId(),
                normalizedFilename
        );
        requireObjectStorageService().put(new PutObjectCommand(objectKey, content, size, resolvedContentType));
        return new MessageAttachmentUploadResult(
                objectKey,
                fileReferenceApi.shareKeyForObjectKey(objectKey),
                normalizedFilename,
                resolvedContentType,
                size
        );
    }

    /**
     * 规范化附件上传对应的消息类型。
     * 约束：空值默认按 file 处理，非 file/voice 类型不能上传附件。
     *
     * @param messageType 客户端提交的消息类型
     * @return 规范化后的附件消息类型
     */
    private String normalizeAttachmentMessageType(String messageType) {
        String normalized = messageType == null || messageType.isBlank()
                ? FILE_MESSAGE_TYPE
                : messageType.trim().toLowerCase();
        if (!FILE_MESSAGE_TYPE.equals(normalized) && !VOICE_MESSAGE_TYPE.equals(normalized)) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, "message_type must be file or voice");
        }
        return normalized;
    }

    /**
     * 解析附件上传内容类型。
     * 语义：客户端未声明时，voice 使用音频默认类型，其它附件使用通用二进制类型。
     *
     * @param messageType 已规范化消息类型
     * @param contentType 客户端提交的内容类型
     * @return 最终写入对象存储的内容类型
     */
    private String resolveContentType(String messageType, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.trim();
        }
        return VOICE_MESSAGE_TYPE.equals(messageType) ? "audio/mpeg" : "application/octet-stream";
    }

    /**
     * 获取对象存储服务实例。
     * 失败语义：运行时未装配对象存储实现时抛出服务不可用问题。
     *
     * @return 可用的对象存储服务
     */
    private ObjectStorageService requireObjectStorageService() {
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            throw ProblemException.of(MessageProblem.STORAGE_UNAVAILABLE);
        }
        return objectStorageService;
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, fieldName + " must be greater than 0");
        }
    }
}
