package team.carrypigeon.backend.api.bo.domain.channel.read;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Channel read state for a user.
 * <p>
 * Used to persist the latest read message position of a user in a channel,
 * so that multiple clients of the same user can synchronize read/unread state.
 * <p>
 * The position is represented only by a timestamp in milliseconds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelReadState {

    /** Primary id */
    private long id;
    /** User id */
    private long uid;
    /** Channel id */
    private long cid;
    /** Latest read message id in the channel (0 means never read) */
    private long lastReadMid;
    /** Latest read time in the channel (epoch millis, 0 means never read) */
    private long lastReadTime;
}
