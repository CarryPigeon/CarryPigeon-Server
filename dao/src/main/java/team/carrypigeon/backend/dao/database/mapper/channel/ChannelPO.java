package team.carrypigeon.backend.dao.database.mapper.channel;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("channel")
public class ChannelPO {
    // 通道id
    @TableId
    private Long id;
    //  通道名
    private String name;
    // 通道所有者
    private Long owner;
    // 通道简介
    private String brief;
    // 通道头像资源id
    private Long avatar;
    // 通道创建时间
    private LocalDateTime createTime;
}
