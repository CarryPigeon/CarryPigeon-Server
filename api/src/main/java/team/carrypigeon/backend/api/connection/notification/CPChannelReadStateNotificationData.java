package team.carrypigeon.backend.api.connection.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Notification payload for channel message read state updates.
 * <p>
 * Pushed to all active sessions of a user when the read state is updated.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPChannelReadStateNotificationData {

    /** Channel id */
    private long cid;

    /** User id */
    private long uid;

    /** Latest read time (epoch millis) */
    private long lastReadTime;
}

