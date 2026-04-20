package team.carrypigeon.backend.infrastructure.service.storage.api.model;

/**
 * 删除对象命令。
 * 职责：承载对象删除所需的稳定对象键。
 *
 * @param objectKey 对象键
 */
public record DeleteObjectCommand(String objectKey) {

    public DeleteObjectCommand {
        PutObjectCommand.validateObjectKey(objectKey);
    }
}
