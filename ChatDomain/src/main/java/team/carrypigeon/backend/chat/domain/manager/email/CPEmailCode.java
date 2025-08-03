package team.carrypigeon.backend.chat.domain.manager.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPEmailCode {
    // 验证码
    private int code;
    // 创建时间
    private LocalDateTime time;

    public boolean isValid() {
        return ChronoUnit.SECONDS.between(time, LocalDateTime.now())>300;
    }
}
