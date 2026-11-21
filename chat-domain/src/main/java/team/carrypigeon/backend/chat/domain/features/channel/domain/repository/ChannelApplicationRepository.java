package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;
import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;

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
     * 分页查询频道下的申请记录。
     * 输出：按申请 ID 倒序返回。
     *
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页申请记录
     */
    PageResult<ChannelApplication> page(long channelId, PageQuery page);

    /**
     * 保存新的申请记录。
     * 约束：调用方负责生成申请 ID，本方法不做去重。
     *
     * @param application 待保存申请记录
     */
    void save(ChannelApplication application);

    /**
     * 仅在指定频道的申请仍为 PENDING 时写入审批结果和响应时间。
     * 调用方先持有频道治理锁；与批准后的成员写入必须处于同一事务。
     *
     * @param application 审批后的申请，状态为 ACCEPTED 或 DECLINED，响应时间非空
     * @return 取得本次审批权时为 true；申请不存在、频道不符或已处理时为 false
     */
    boolean decideIfPending(ChannelApplication application);

    /**
     * 物理删除申请记录。
     * 用途：重新申请时先删除该账号在同一频道的历史记录，以维持"至多一行"的不变量。
     *
     * @param applicationId 申请 ID
     */
    void delete(long applicationId);

    /**
     * 删除频道下的全部申请记录。
     * 用途：频道删除时一次性清理依赖数据，避免先查全量再逐条删除。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);

    /**
     * 分页查询申请者发起的申请记录。
     * 输出：按申请 ID 倒序返回。
     *
     * @param operatorAccountId 申请者账户 ID
     * @param page 分页参数
     * @return 分页申请记录
     */
    PageResult<ChannelApplication> pageMe(long operatorAccountId, PageQuery page);
}
