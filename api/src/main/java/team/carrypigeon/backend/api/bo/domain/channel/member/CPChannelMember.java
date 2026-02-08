package team.carrypigeon.backend.api.bo.domain.channel.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 频道成员关系领域对象。
 * <p>
 * 表示某用户加入某频道后的成员信息（群昵称、权限、加入时间等）。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelMember {
    /**
     * 成员关系 ID。
     */
    private long id;
    /**
     * 用户 ID。
     */
    private long uid;
    /**
     * 频道 ID。
     */
    private long cid;
    /**
     * 群昵称（成员在该频道下的展示名）。
     */
    private String name;
    /**
     * 权限等级。
     */
    private CPChannelMemberAuthorityEnum authority;
    /**
     * 加入时间。
     */
    private LocalDateTime joinTime;
}
