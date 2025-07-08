package team.carrypigeon.backend.dao.mapper.msg;

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
public class CPMessagePO {
    @TableId
    private long id;
    private long sendUserId;
    private long toId;
    private String domain;
    private int type;
    private String data;
    private LocalDateTime sendTime;
}
