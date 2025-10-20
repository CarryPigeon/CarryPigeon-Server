package team.carrypigeon.backend.dao.mapper.token;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_token")
public class TokenPO {
    private Long id;
    private Long uid;
    private String token;
    private String deviceName;
    private LocalDateTime time;
}