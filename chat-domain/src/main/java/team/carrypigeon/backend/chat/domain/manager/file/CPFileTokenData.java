package team.carrypigeon.backend.chat.domain.manager.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPFileTokenData {
    private LocalDateTime applyTime;
    private Object data;
}
