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
 * `channel_application` 表持久化对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel_application")
public class ChannelApplicationPO {

    /**
     * 申请记录 ID。
     */
    @TableId
    private Long id;

    /**
     * 申请人用户 ID。
     */
    private Long uid;

    /**
     * 频道 ID。
     */
    private Long cid;

    /**
     * 申请状态值。
     */
    private int state;

    /**
     * 申请留言。
     */
    private String msg;

    /**
     * 申请时间。
     */
    private LocalDateTime applyTime;

    /**
     * 将 PO 转换为 BO。
     *
     * @return 频道申请领域对象。
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
     * 从 BO 构建 PO。
     *
     * @param cpChannelApplication 频道申请领域对象。
     * @return 频道申请持久化对象。
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
