package team.carrypigeon.backend.dao.mapper.channel;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("channel_type")
public class ChannelTypePO {
    @TableId
    private long id;
    private long channelId;
    private String type;
}
