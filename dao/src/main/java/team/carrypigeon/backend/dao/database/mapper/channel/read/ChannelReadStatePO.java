package team.carrypigeon.backend.dao.database.mapper.channel.read;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;

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
     * Latest read time in the channel (epoch millis).
     */
    private Long lastReadTime;

    public static ChannelReadStatePO fromBo(CPChannelReadState state) {
        if (state == null) {
            return null;
        }
        return new ChannelReadStatePO()
                .setId(state.getId())
                .setUid(state.getUid())
                .setCid(state.getCid())
                .setLastReadTime(state.getLastReadTime());
    }

    public CPChannelReadState toBo() {
        return new CPChannelReadState()
                .setId(id == null ? 0L : id)
                .setUid(uid == null ? 0L : uid)
                .setCid(cid == null ? 0L : cid)
                .setLastReadTime(lastReadTime == null ? 0L : lastReadTime);
    }
}
