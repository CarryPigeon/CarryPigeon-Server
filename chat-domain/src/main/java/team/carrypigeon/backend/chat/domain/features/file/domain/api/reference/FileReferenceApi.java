package team.carrypigeon.backend.chat.domain.features.file.domain.api.reference;

/** 文件引用业务入口；由资源写入事务调用，引用与资源本身一起提交或回滚。 */
public interface FileReferenceApi {
    /**
     * 替换用户头像引用；调用前须持有该用户资料行锁，并处于数据库事务内。
     * 本服务下载路径中的 share key 作为文件引用；其它外部头像地址不登记本地文件。
     * 引用目标必须已完成且尚未回收，最后一个引用解除后立即进入回收状态。
     */
    void replaceAvatarReference(long accountId, String avatarUrl);
}
