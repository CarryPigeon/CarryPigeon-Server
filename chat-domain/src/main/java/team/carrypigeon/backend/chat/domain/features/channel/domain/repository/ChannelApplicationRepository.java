package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelApplication;

/**
 * 频道申请仓储抽象。
 * 职责：定义频道邀请记录的领域语义读写入口。
 * 边界：不暴露数据库实现细节，也不把邀请压进活跃成员状态机。
 */
public interface ChannelApplicationRepository {

    /**
     * 按申请ID 查询。
     *
     * @return 申请记录，如果不存在则返回空Optional
     */
    Optional<ChannelApplication> findById(long applicationId);

    /**
     * 根据频道和申请人查找申请记录。
     *
     * @param channelId   频道id
     * @param applicantId 申请人id
     * @return 申请记录，如果不存在则返回空Optional
     */
    Optional<ChannelApplication> findByRelation(long channelId, long applicantId);

    /**
     * TODO 分页查询
     * 查询频道下的全部申请记录。
     *
     * @return 申请的列表
     */
    List<ChannelApplication> list(long channelId);

    /**
     * 保存新的申请记录。
     *
     * @param application 申请记录
     */
    void save(ChannelApplication application);

    /**
     * 更新已存在的申请记录。
     *
     * @param application 申请记录
     */
    void update(ChannelApplication application);

    /**
     * 删除申请
     *
     * @param applicationId 申请id
     * */
    void delete(long applicationId);
}
