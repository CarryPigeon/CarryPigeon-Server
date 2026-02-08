package team.carrypigeon.backend.dao.database.mapper.channel.read;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;

/**
 * 频道消息已读状态的持久化对象（MyBatis-Plus PO）。
 * <p>
 * 用于在数据库表 {@code channel_read_state} 中存取 (uid, cid) 的最后已读时间戳。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel_read_state")
public class ChannelReadStatePO {

    @TableId
    private Long id;

    private Long uid;

    private Long cid;

    /**
     * Latest read message id in the channel (0 means never read).
     */
    private Long lastReadMid;

    /**
     * Latest read time in the channel (epoch millis).
     */
    private Long lastReadTime;

    /**
     * 从领域对象转换为 PO。
     */
    public static ChannelReadStatePO fromBo(CPChannelReadState state) {
        if (state == null) {
            return null;
        }
        return new ChannelReadStatePO()
                .setId(state.getId())
                .setUid(state.getUid())
                .setCid(state.getCid())
                .setLastReadMid(state.getLastReadMid())
                .setLastReadTime(state.getLastReadTime());
    }

    /**
     * 转换为领域对象（空字段会按 0 兜底）。
     */
    public CPChannelReadState toBo() {
        return new CPChannelReadState()
                .setId(id == null ? 0L : id)
                .setUid(uid == null ? 0L : uid)
                .setCid(cid == null ? 0L : cid)
                .setLastReadMid(lastReadMid == null ? 0L : lastReadMid)
                .setLastReadTime(lastReadTime == null ? 0L : lastReadTime);
    }
}
