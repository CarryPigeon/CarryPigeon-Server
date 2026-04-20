package team.carrypigeon.backend.infrastructure.service.storage.api.service;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;

/**
 * 对象存储服务抽象。
 * 职责：表达上传、读取、删除和预签名 URL 的稳定对象存储能力。
 * 边界：不暴露 MinIO 客户端、桶创建规则或具体 SDK 类型。
 */
public interface ObjectStorageService {

    /**
     * 上传对象内容。
     *
     * @param command 上传命令
     * @return 上传后的对象信息
     */
    StorageObject put(PutObjectCommand command);

    /**
     * 读取对象信息和内容入口。
     *
     * @param command 读取命令
     * @return 对象不存在时返回空
     */
    Optional<StorageObject> get(GetObjectCommand command);

    /**
     * 删除指定对象。
     *
     * @param command 删除命令
     */
    void delete(DeleteObjectCommand command);

    /**
     * 创建对象访问预签名 URL。
     *
     * @param command 预签名 URL 命令
     * @return 预签名 URL
     */
    PresignedUrl createPresignedUrl(PresignedUrlCommand command);
}
