package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.DiscoveredChannel;

/**
 * 频道仓储抽象。
 * 职责：以业务语义表达频道聚合的读写入口，读写最终经 database-api 完成。
 * 边界：不暴露数据库实现细节、不承载可见性与权限规则；不负责成员关系、禁言与申请记录。
 */
public interface ChannelRepository {

    /** 登记删除任务，与移除频道主记录同事务执行。 */
    void scheduleDeletion(long channelId, long operatorAccountId);

    /** 事务内当前读取得删除发起者，用于原所有者幂等重试。 */
    Optional<Long> findDeletionOperator(long channelId);

    /** 锁定一个未完成删除任务，必须在事务中调用，跳过其它工作者持有的任务。 */
    Optional<Long> findPendingDeletionForUpdate();

    /** 在最后一批数据清理成功的事务中登记完成，不删除幂等重试依据。 */
    void completeDeletion(long channelId);


    /**
     * 为授权判断直接读取数据库，绕过最终一致的展示缓存。
     * 返回当前事务可见的记录；不存在时为空，不提供检查与后续写入之间的并发保证。
     */
    Optional<Channel> findByIdForAuthorization(long channelId);

    /**
     * 锁定频道并读取最新记录，不经过缓存；频道不存在时返回空。
     * 必须在事务内调用，锁持续到事务结束；治理写入先锁频道，再读取成员角色和修改关联数据。
     */
    Optional<Channel> findByIdForUpdate(long channelId);

    /**
     * 查询全部系统频道。
     * 用途：账号初始化时为其建立默认频道的成员关系。
     *
     * @return 系统频道列表；无系统频道时返回空列表
     */
    List<Channel> listSystemChannels();

    /**
     * 按频道 ID 查询展示数据，允许命中缓存；授权存在性检查使用 findByIdForAuthorization。
     *
     * @param channelId 频道 ID
     * @return 命中时返回频道，未命中时返回空 Optional
     */
    Optional<Channel> findById(long channelId);

    /**
     * 按频道 ID 集合批量查询频道。
     * 约束：调用方需保证集合非空，空集合会产生无意义的空 IN 条件。
     *
     * @param channelIds 频道 ID 集合
     * @return 命中的频道列表；不保证与入参顺序一致
     */
    List<Channel> listByIds(Collection<Long> channelIds);

    /**
     * 按发现场景规则查询频道。
     * 输入：关键字、游标与查询上限；关键字为空表示不按关键字过滤。
     * 输出：发现读模型列表，包含成员数与加入门槛提示。
     *
     * @param keyword 关键字；为空表示不过滤
     * @param cursorChannelId 游标频道 ID；为空表示从第一页开始
     * @param limit 查询上限，包含用于判定下一页的探测条数
     * @return 频道发现读模型列表
     */
    List<DiscoveredChannel> discoverChannels(String keyword, Long cursorChannelId, int limit);

    /**
     * 保存新频道。
     * 约束：调用方负责生成频道 ID，本方法不做去重。
     *
     * @param channel 待保存频道
     */
    void save(Channel channel);

    /**
     * 更新已存在频道。
     * 约束：按频道 ID 整行覆盖，调用方必须传入保留全部未变更字段的完整频道模型，
     * 尤其是 systemChannel 标记，否则会把系统频道降级为普通频道。
     *
     * @param channel 待更新频道
     */
    void update(Channel channel);

    /**
     * 删除频道。
     * 约束：只删除频道记录本身，不级联删除成员关系、禁言与申请记录，级联清理由调用方编排。
     *
     * @param channelId 频道 ID
     */
    void delete(long channelId);
}
