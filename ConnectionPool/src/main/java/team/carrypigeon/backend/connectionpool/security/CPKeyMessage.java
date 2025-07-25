package team.carrypigeon.backend.connectionpool.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPKeyMessage {
    private long id;
    private String key;
}
