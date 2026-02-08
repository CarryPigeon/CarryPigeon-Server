package team.carrypigeon.backend.api.bo.domain.channel.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 入群申请（频道申请）领域对象。
 * <p>
 * 表示用户申请加入频道的记录及其审批状态。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelApplication {
    /**
     * 申请记录 ID。
     */
    private Long id;
    /**
     * 申请人用户 ID。
     */
    private Long uid;
    /**
     * 申请加入的频道 ID。
     */
    private Long cid;
    /**
     * 申请状态：
     * <ul>
     *     <li>0：待处理</li>
     *     <li>1：通过</li>
     *     <li>2：拒绝</li>
     * </ul>
     */
    private CPChannelApplicationStateEnum state;
    /**
     * 申请留言（可为空）。
     */
    private String msg;
    /**
     * 申请时间。
     */
    private LocalDateTime applyTime;
}
