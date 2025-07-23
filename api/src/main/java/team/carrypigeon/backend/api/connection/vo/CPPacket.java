package team.carrypigeon.backend.api.connection.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 数据传输对象，用户客户端到服务端以及服务端到客户端数据的包装
 * */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPPacket {
    /**
     * 请求id
     * 客户端到服务端的id用于标识响应值对应的请求，如果为-1则为不需要返回
     * 服务端到客户端统一为-1标识，不需要回应
     * */
    private long id;
    /**
     * 分发路径，用于标识消息类型
     * */
    private String route;
    /**
     * 具体的结构数据，统一用jsonNode包装用于使其生成为
     * "data":{}的形式
     * */
    private JsonNode data;
}
