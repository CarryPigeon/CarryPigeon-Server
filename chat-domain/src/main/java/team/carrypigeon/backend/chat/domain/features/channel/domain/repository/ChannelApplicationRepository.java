package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;

/**
 * 频道申请仓储抽象。
 * 职责：以业务语义表达频道入群申请记录的读写入口。
 * 边界：不暴露数据库实现细节，也不把申请记录压进活跃成员状态机；申请审批后的成员关系变更由领域服务编排。
 */
public interface ChannelApplicationRepository {

    /**
     * 按申请 ID 查询申请记录。
     *
     * @param applicationId 申请 ID
     * @return 命中时返回申请记录，未命中时返回空 Optional
     */
    Optional<ChannelApplication> findById(long applicationId);

    /**
     * 按频道与申请人查询申请记录。
     * 约束：实现依赖"同一 (频道, 申请人) 至多一行"这一不变量，因此底层为单行查询；
     * 一旦出现重复行，本方法会直接查询失败而非返回任意一行。
     *
     * @param channelId 频道 ID
     * @param applicantId 申请人账户 ID
     * @return 命中时返回申请记录，未命中时返回空 Optional
     */
    Optional<ChannelApplication> findByRelation(long channelId, long applicantId);

    /**
     * 查询频道下的全部申请记录。
     * 说明：当前不提供分页，调用方直接消费全量列表。
     *
     * @param channelId 频道 ID
     * @return 申请记录列表；频道无申请时返回空列表
     */
    List<ChannelApplication> list(long channelId);

    /**
     * 保存新的申请记录。
     * 约束：调用方负责生成申请 ID，本方法不做去重。
     *
     * @param application 待保存申请记录
     */
    void save(ChannelApplication application);

    /**
     * 更新已存在的申请记录。
     * 约束：按申请主键更新，调用方必须传入保留原主键的申请模型。
     *
     * @param application 待更新申请记录
     */
    void update(ChannelApplication application);

    /**
     * 物理删除申请记录。
     * 用途：重新申请时先删除该账号在同一频道的历史记录，以维持"至多一行"的不变量。
     *
     * @param applicationId 申请 ID
     */
    void delete(long applicationId);
}
