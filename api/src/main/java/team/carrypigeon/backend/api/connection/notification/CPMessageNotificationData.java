package team.carrypigeon.backend.api.connection.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPMessageNotificationData {
    private String sContent;
    private long cid;
    private long uid;
    private long sendTime;
}
