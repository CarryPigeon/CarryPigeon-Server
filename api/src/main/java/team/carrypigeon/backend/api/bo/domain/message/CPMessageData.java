package team.carrypigeon.backend.api.bo.domain.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 聊天消息的数据，根据具体数据类型由服务端自行处理结构
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
public class CPMessageData {
    ///  类型id，由核心域或者插件域自行决定
    private int type;
    ///  数据
    private JsonNode data;
}
