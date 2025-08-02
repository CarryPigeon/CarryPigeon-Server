package team.carrypigeon.backend.dao.mapper.key;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_key")
public class KeyPO {
    private Long id;
    private Long uid;
    private String loginKey;
    private LocalDateTime time;
}
