package team.carrypigeon.backend.api.connection.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Notification payload for channel member changes.
 * <p>
 * Used by pushes on route "/core/channel/member/list".
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPChannelMemberNotificationData {

    /**
     * Change type, e.g.:
     * - "join"        : a user joined the channel
     * - "leave"       : a member was removed from the channel
     * - "admin_add"   : a member was promoted to admin
     * - "admin_remove": a member was demoted from admin
     */
    private String type;

    /** Channel id */
    private long cid;

    /** User id of the affected member */
    private long uid;
}

