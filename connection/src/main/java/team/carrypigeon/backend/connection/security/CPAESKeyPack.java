package team.carrypigeon.backend.connection.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPAESKeyPack {
    private long id;
    private long sessionId;
    private String key;
}
