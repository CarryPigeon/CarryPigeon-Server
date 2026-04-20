package team.carrypigeon.backend.infrastructure.service.storage.api.model;

/**
 * 读取对象命令。
 * 职责：承载对象读取所需的稳定对象键。
 *
 * @param objectKey 对象键
 */
public record GetObjectCommand(String objectKey) {

    public GetObjectCommand {
        PutObjectCommand.validateObjectKey(objectKey);
    }
}
