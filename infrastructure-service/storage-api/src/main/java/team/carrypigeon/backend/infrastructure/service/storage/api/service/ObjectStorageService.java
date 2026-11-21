package team.carrypigeon.backend.infrastructure.service.storage.api.service;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;

/**
 * 对象存储服务抽象。
 * 职责：表达上传、读取、删除和预签名 URL 的稳定对象存储能力。
 * 边界：不暴露 MinIO 客户端、桶创建规则或具体 SDK 类型。
 */
public interface ObjectStorageService {

    /**
     * 上传对象内容
     *
     * @param objectKey 对象键
     * @param content   对象内容输入流
     * @param size      对象内容大小
     * @param contentType 对象内容类型
     * @param sha256 对象内容的sha256
     */
    StorageObject put(String objectKey, InputStream content, long size, String contentType,String sha256);

    /**
     * 读取对象元信息及内容。
     *
     * @param objectKey 对象键
     * @return 对象不存在时返回空；存在时内容流必须存在，调用方负责关闭
     */
    Optional<StorageObject> get(String objectKey);

    /**
     * 删除指定对象。
     *
     * @param objectKey 对象键
     */
    void delete(String objectKey);

    /**
     * 创建对象访问预签名 URL。
     *
     * @param objectKey 对象键
     * @return 预签名 URL
     */
    PresignedUrl createPresignedUrl(String objectKey, Duration ttl);
}
