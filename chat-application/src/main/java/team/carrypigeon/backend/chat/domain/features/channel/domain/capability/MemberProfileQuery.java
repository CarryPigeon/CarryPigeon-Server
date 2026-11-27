package team.carrypigeon.backend.chat.domain.features.channel.domain.capability;

import java.util.Collection;
import java.util.Map;

/**
 * 频道成员展示资料查询能力。
 * 职责：为频道视图提供成员身份字段，隔离资料来源 feature。
 */
public interface MemberProfileQuery {

    /**
     * 批量查询成员展示资料。
     * 输出：账号 ID 到资料的映射；资料缺失的账号不出现在结果中。
     *
     * @param accountIds 目标账号 ID
     * @return 账号 ID 到展示资料的映射
     */
    Map<Long, MemberProfile> findProfiles(Collection<Long> accountIds);

    /**
     * 查询单个成员展示资料。
     * 失败语义：资料不存在时由实现抛出领域问题异常。
     *
     * @param accountId 目标账号 ID
     * @return 成员展示资料
     */
    MemberProfile findProfile(long accountId);
}
