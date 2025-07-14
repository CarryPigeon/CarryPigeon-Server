package team.carrypigeon.backend.api.domain.bo.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageBO {
    private long id;
    private long sendUserId;
    private long toId;
    private CPMessageDomain domain;
    private CPMessageData data;
    private LocalDateTime sendTime;
}