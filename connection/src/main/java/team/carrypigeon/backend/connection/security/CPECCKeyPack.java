package team.carrypigeon.backend.connection.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPECCKeyPack {
    private long id;
    private String key;
}
