package team.carrypigeon.backend.dao.database.mapper.channel.application;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;

import java.time.LocalDateTime;

/**
 * {@code channel_application} 表的持久化对象（PO）。
 * <p>
 * state 字段为枚举值（见 {@link CPChannelApplicationStateEnum}）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel_application")
public class ChannelApplicationPO {
    // 申请表id
    @TableId
    private Long id;
    // 申请人id
    private Long uid;
    // 申请通道id
    private Long cid;
    // 申请状态，0为待处理，1为通过，2为拒绝
    private int state;
    // 申请信息留言
    private String msg;
    // 申请时间
    private LocalDateTime applyTime;

    /**
     * 将当前 PO 转换为领域对象（BO）。
     */
    public CPChannelApplication toBo() {
        return new CPChannelApplication()
                .setId(id)
                .setUid(uid)
                .setCid(cid)
                .setState(CPChannelApplicationStateEnum.valueOf(state))
                .setMsg(msg)
                .setApplyTime(applyTime);
    }

    /**
     * 从领域对象（BO）创建 PO。
     */
    public static ChannelApplicationPO fromBo(CPChannelApplication cpChannelApplication) {
        return new ChannelApplicationPO()
                .setId(cpChannelApplication.getId())
                .setUid(cpChannelApplication.getUid())
                .setCid(cpChannelApplication.getCid())
                .setState(cpChannelApplication.getState().getValue())
                .setMsg(cpChannelApplication.getMsg())
                .setApplyTime(cpChannelApplication.getApplyTime());
    }
}
