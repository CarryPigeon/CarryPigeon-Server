package team.carrypigeon.backend.api.bo.domain.user;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CarryPigeon用户端详，映射为聊天域中一个具体用户
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPUserBO {
    private long id;
    private String name;
    private String email;
    private JsonNode data;
    private LocalDateTime registerTime;
    private long stateId;
}