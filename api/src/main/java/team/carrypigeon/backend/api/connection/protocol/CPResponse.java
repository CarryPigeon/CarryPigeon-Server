package team.carrypigeon.backend.api.connection.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 业务响应包（解密后的 JSON 协议体）。
 * <p>
 * 对客户端请求的响应格式：
 * <pre>
 * { "id": <requestId>, "code": 200|100|300|404|500, "data": { ... } }
 * </pre>
 *
 * <p>推送（通知）格式（服务端主动下发，不需要客户端回应）：
 * <pre>
 * { "id": -1, "code": 0, "data": { "route": "...", "data": { ... } } }
 * </pre>
 *
 * <p>code 约定：
 * <ul>
 *     <li>200：成功</li>
 *     <li>100：参数/业务错误</li>
 *     <li>300：权限错误（典型：未登录）</li>
 *     <li>404：路由不存在</li>
 *     <li>500：服务器内部错误</li>
 *     <li>0：推送通知（仅当 {@link #id} 为 -1 时使用）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPResponse {

    /** 推送（通知）外层固定 id。 */
    public static final long PUSH_ID = -1L;

    /** 推送（通知）外层固定 code。 */
    public static final int CODE_NOTIFICATION = 0;

    /** 成功。 */
    public static final int CODE_SUCCESS = 200;

    /** 参数/业务错误。 */
    public static final int CODE_ERROR = 100;

    /** 权限错误（典型：未登录）。 */
    public static final int CODE_AUTHORITY_ERROR = 300;

    /** 路由不存在。 */
    public static final int CODE_PATH_NOT_FOUND = 404;

    /** 服务器内部错误。 */
    public static final int CODE_SERVER_ERROR = 500;

    /** 兼容保留：服务器错误模板（code=500）。请优先使用 {@link #serverError()}。 */
    public static final CPResponse SERVER_ERROR = new CPResponse(PUSH_ID, CODE_SERVER_ERROR, null);
    /**
     * 响应 id。
     * <ul>
     *     <li>响应请求：等于请求 {@code CPPacket.id}</li>
     *     <li>服务端推送：固定为 -1</li>
     * </ul>
     */
    private long id;
    /**
     * 状态码（详见类注释）。
     */
    private int code;
    /**
     * 响应数据体（JSON）。
     * <p>
     * 常见约定：
     * <ul>
     *     <li>错误响应：{@code {"msg":"..."}}</li>
     *     <li>推送通知：{@code CPNotification} 对象</li>
     * </ul>
     */
    private JsonNode data;

    /**
     * 兼容保留：错误响应模板（code=100）。请优先使用 {@link #error()}。
     */
    @JsonIgnore
    public static final CPResponse ERROR_RESPONSE = new CPResponse(PUSH_ID, CODE_ERROR, null);

    /**
     * 兼容保留：成功响应模板（code=200）。请优先使用 {@link #success()}。
     */
    @JsonIgnore
    public static final CPResponse SUCCESS_RESPONSE = new CPResponse(PUSH_ID, CODE_SUCCESS, null);

    /**
     * 兼容保留：权限错误响应模板（code=300）。请优先使用 {@link #authorityError()}。
     */
    @JsonIgnore
    public static final CPResponse AUTHORITY_ERROR_RESPONSE = new CPResponse(PUSH_ID, CODE_AUTHORITY_ERROR, null);
    /**
     * 兼容保留：路由不存在响应模板（code=404）。请优先使用 {@link #pathNotFound()}。
     */
    @JsonIgnore
    public static final CPResponse PATH_NOT_FOUND_RESPONSE = new CPResponse(PUSH_ID, CODE_PATH_NOT_FOUND, null);

    /**
     * 工厂方法：创建一个新的错误响应模板（code=100，id=-1，data=null）。
     */
    public static CPResponse error() {
        return new CPResponse(PUSH_ID, CODE_ERROR, null);
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
        return new CPResponse(PUSH_ID, CODE_SUCCESS, null);
    }

    /**
     * 工厂方法：创建一个新的权限错误响应模板（code=300，id=-1，data=null）。
     */
    public static CPResponse authorityError() {
        return new CPResponse(PUSH_ID, CODE_AUTHORITY_ERROR, null);
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
        return new CPResponse(PUSH_ID, CODE_PATH_NOT_FOUND, null);
    }

    /**
     * 工厂方法：创建一个新的服务器错误响应模板（code=500，id=-1，data=null）。
     */
    public static CPResponse serverError() {
        return new CPResponse(PUSH_ID, CODE_SERVER_ERROR, null);
    }

    /**
     * 工厂方法：创建一个带错误消息的服务器错误响应。
     */
    public static CPResponse serverError(String message) {
        return serverError().setTextData(message);
    }

    /**
     * 工厂方法：创建一个推送（通知）响应（id=-1，code=0）。
     *
     * @param notification 推送通知数据（通常为 {@code CPNotification} 的 JSON 树）
     */
    public static CPResponse notification(JsonNode notification) {
        return new CPResponse(PUSH_ID, CODE_NOTIFICATION, notification);
    }
    /**
     * 兼容保留：浅拷贝当前响应对象。
     * <p>
     * 注意：该方法不会深拷贝 {@link #data}，可能导致共享引用。
     */
    @Deprecated
    public CPResponse copy() {
        CPResponse clone = new CPResponse();
        clone.setId(this.id);
        clone.setCode(this.code);
        clone.setData(this.data);
        return clone;
    }

    /**
     * 将当前响应的 {@link #data} 设置为 {@code {"msg": "..."} } 结构。
     * <p>
     * 用于快速返回“文本错误/提示信息”，与各业务 Result 的结构无关。
     * </p>
     */
    public CPResponse setTextData(String content) {
        ObjectNode msg = JsonNodeFactory.instance.objectNode().put("msg", content);
        return this.setData(msg);
    }
}
