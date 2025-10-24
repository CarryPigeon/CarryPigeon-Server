package team.carrypigeon.backend.api.connection.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 通知，用于服务端向客户端发送通知数据
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPNotification {
    private String route;
    private JsonNode data;
}
