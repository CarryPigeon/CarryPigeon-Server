package team.carrypigeon.backend.dao.database.mapper.message;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据库中消息的映射类
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("message")
public class MessagePO {
    // 消息id
    @TableId
    private Long id;
    // 用户id
    private Long uid;
    // 通道id
    private Long cid;
    // 消息域，格式为 Domain:SubDomain
    private String domain;
    // 消息数据
    private String data;
    // 消息状态
    private int state;
    // 发送时间
    private LocalDateTime sendTime;
}
