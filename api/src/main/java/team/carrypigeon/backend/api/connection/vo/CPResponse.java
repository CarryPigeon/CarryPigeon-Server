package team.carrypigeon.backend.api.connection.vo;

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
    /**
     * 返回值id，用于客户端标识为哪个请求的响应
     * */
    private long id;
    /**
     * 响应code
     * */
    private int code;
    /**
     * 响应数据
     * */
    private JsonNode data;

    /**
     * 错误数据的标准响应值
     * */
    @JsonIgnore
    public static CPResponse ERROR_RESPONSE = new CPResponse(-1,100,null);

    /**
     * 成功数据标准响应值
     * */
    @JsonIgnore
    public static CPResponse SUCCESS_RESPONSE = new CPResponse(-1,200,null);

    /**
     * 路径不存在response
     * */
    @JsonIgnore
    public static CPResponse PATH_NOT_FOUND_RESPONSE = new CPResponse(-1,404,null);
    /**
     * 主要用于copy错误与正确的响应修改id标识
     * */
    public CPResponse copy() {
        CPResponse clone = new CPResponse();
        clone.setId(this.id);
        clone.setCode(this.code);
        clone.setData(this.data);
        return clone;
    }
}