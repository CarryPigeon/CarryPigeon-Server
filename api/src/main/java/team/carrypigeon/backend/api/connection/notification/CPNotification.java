package team.carrypigeon.backend.api.connection.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知，用于服务端向客户端发送通知数据
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPNotification {
    private String route;
    private JsonNode data;
}
