package team.carrypigeon.backend.commander;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestClientState {
    private State state =  State.WAITE_RECEIVE_KEY;
    private String aesKey;
    private String ECCKey;
}
