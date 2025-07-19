package team.carrypigeon.backend.common.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 返回值标准格式，详细请见
 * <a href="https://www.yuque.com/cpointerz/cp/ngfaxwn1vrpr121v#Y3yvv">通用数据交换协议及简介</a>
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPResponse{
    private long id;
    private int code;
    private JsonNode data;

    @JsonIgnore
    public static CPResponse ERROR_RESPONSE = new CPResponse(-1,100,null);

    @JsonIgnore
    public static CPResponse SUCCESS_RESPONSE = new CPResponse(-1,200,null);

    public CPResponse copy() {
        CPResponse clone = new CPResponse();
        clone.setId(this.id);
        clone.setCode(this.code);
        clone.setData(this.data);
        return clone;
    }
}