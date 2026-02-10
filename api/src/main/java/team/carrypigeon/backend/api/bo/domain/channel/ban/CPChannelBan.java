package team.carrypigeon.backend.api.bo.domain.channel.ban;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 频道禁言记录领域对象。
 * <p>
 * 用于描述某用户在某频道下的禁言状态（管理员、时长、起始时间）。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelBan {
    /**
     * 禁言记录 ID。
     */
    private long id;
    /**
     * 频道 ID。
     */
    private long cid;
    /**
     * 被禁言用户 ID。
     */
    private long uid;
    /**
     * 执行禁言的管理员用户 ID。
     */
    private long aid;
    /**
     * 禁言时长（单位：秒）。
     */
    private int duration;
    /**
     * 禁言截止时间（毫秒时间戳）。
     * <p>
     * 这是对外 API 更友好的表达方式；当该值大于 0 时，优先使用它判断有效期。
     */
    private long untilTime;
    /**
     * 禁言原因（可为空）。
     */
    private String reason;
    /**
     * 禁言创建时间（开始时间）。
     */
    private LocalDateTime createTime;

    /**
     * 判断禁言是否仍在有效期内。
     *
     *  仍在禁言期返回 { true}，否则返回 { false}。
     */
    public boolean isValid() {
        if (untilTime > 0) {
            return untilTime > System.currentTimeMillis();
        }
        return createTime != null && !LocalDateTime.now().isAfter(createTime.plusSeconds(duration));
    }
}
