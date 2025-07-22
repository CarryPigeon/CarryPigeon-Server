package team.carrypigeon.backend.dao.mapper.message;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("message")
public class MessagePO {
    @TableId
    private Long id;
    private Long sendUserId;
    private Long toId;
    private String domain;
    private Integer type;
    private String data;
    private LocalDateTime sendTime;
}
