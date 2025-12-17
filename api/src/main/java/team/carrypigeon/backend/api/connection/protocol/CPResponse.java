package team.carrypigeon.backend.api.connection.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.core.util.JsonUtils;

/**
 * 返回值标准格式，详细请见
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPResponse{
    public static final CPResponse SERVER_ERROR = new CPResponse(-1,500,null);
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
    public static final CPResponse ERROR_RESPONSE = new CPResponse(-1,100,null);

    /**
     * 成功数据标准响应值
     * */
    @JsonIgnore
    public static final CPResponse SUCCESS_RESPONSE = new CPResponse(-1,200,null);

    /**
     * 权限校验错误
     * */
    @JsonIgnore
    public static final CPResponse AUTHORITY_ERROR_RESPONSE = new CPResponse(-1,300,null);
    /**
     * 路径不存在response
     * */
    @JsonIgnore
    public static final CPResponse PATH_NOT_FOUND_RESPONSE = new CPResponse(-1,404,null);

    /**
     * 工厂方法：创建一个新的错误响应模板（code=100，id=-1，data=null）。
     */
    public static CPResponse error() {
        return new CPResponse(-1, 100, null);
    }

    /**
     * 工厂方法：创建一个带错误消息的错误响应。
     */
    public static CPResponse error(String message) {
        return error().setTextData(message);
    }

    /**
     * 工厂方法：创建一个新的成功响应模板（code=200，id=-1，data=null）。
     */
    public static CPResponse success() {
        return new CPResponse(-1, 200, null);
    }

    /**
     * 工厂方法：创建一个新的权限错误响应模板（code=300，id=-1，data=null）。
     */
    public static CPResponse authorityError() {
        return new CPResponse(-1, 300, null);
    }

    /**
     * 工厂方法：创建一个带错误消息的权限错误响应。
     */
    public static CPResponse authorityError(String message) {
        return authorityError().setTextData(message);
    }

    /**
     * 工厂方法：创建一个新的路径不存在响应模板（code=404，id=-1，data=null）。
     */
    public static CPResponse pathNotFound() {
        return new CPResponse(-1, 404, null);
    }

    /**
     * 工厂方法：创建一个新的服务器错误响应模板（code=500，id=-1，data=null）。
     */
    public static CPResponse serverError() {
        return new CPResponse(-1, 500, null);
    }

    /**
     * 工厂方法：创建一个带错误消息的服务器错误响应。
     */
    public static CPResponse serverError(String message) {
        return serverError().setTextData(message);
    }
    /**
     * 主要用于copy错误与正确的响应修改id标识
     * */
    @Deprecated
    public CPResponse copy() {
        CPResponse clone = new CPResponse();
        clone.setId(this.id);
        clone.setCode(this.code);
        clone.setData(this.data);
        return clone;
    }

    public CPResponse setTextData(String content){
        ObjectNode msg = JsonNodeFactory.instance.objectNode().put("msg", content);
        return this.setData(msg);
    }
}
