package team.carrypigeon.backend.connectionpool.security.ecc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RsaKeyPair {
    private String publicKey;
    private String privateKey;
}
