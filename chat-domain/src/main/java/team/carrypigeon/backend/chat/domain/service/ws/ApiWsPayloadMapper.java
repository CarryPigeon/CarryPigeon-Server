package team.carrypigeon.backend.chat.domain.service.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.service.preview.ApiMessagePreviewService;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * WS payload 映射器：将领域对象转换为 WS 事件的 payload。
 * <p>
 * 约束：
 * <ul>
 *   <li>所有雪花 ID 在 JSON 中用十进制字符串编码</li>
 *   <li>payload 字段命名为 snake_case</li>
 *   <li>图片/文件地址返回相对路径</li>
 * </ul>
 */
@Service
public class ApiWsPayloadMapper {

    private final ObjectMapper objectMapper;
    private final UserDao userDao;
    private final FileInfoDao fileInfoDao;
    private final ApiMessagePreviewService previewService;

    /**
     * 构造 WS 负载映射器。
     *
     * @param objectMapper JSON 映射器
     * @param userDao 用户数据访问
     * @param fileInfoDao 文件数据访问
     * @param previewService 消息预览服务
     */
    public ApiWsPayloadMapper(ObjectMapper objectMapper, UserDao userDao, FileInfoDao fileInfoDao, ApiMessagePreviewService previewService) {
        this.objectMapper = objectMapper;
        this.userDao = userDao;
        this.fileInfoDao = fileInfoDao;
        this.previewService = previewService;
    }

    /**
     * 构造 {@code message.created} 的 payload。
     * <p>
     * 结构：
     * <pre>{@code
     * {
     *   "cid": "<snowflake_string>",
     *   "message": { ... }
     * }
     * }</pre>
     */
    public ObjectNode messageCreatedPayload(CPMessage message) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cid", Long.toString(message.getCid()));
        payload.set("message", messageItem(message));
        return payload;
    }

    /**
     * 构造 {@code message.deleted} 的 payload。
     * <p>
     * 结构：{@code {"cid":"...","mid":"...","delete_time":<epoch_millis>}}
     */
    public ObjectNode messageDeletedPayload(long cid, long mid, long deleteTimeMillis) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cid", Long.toString(cid));
        payload.put("mid", Long.toString(mid));
        payload.put("delete_time", deleteTimeMillis);
        return payload;
    }

    /**
     * 构造 {@code read_state.updated} 的 payload。
     * <p>
     * 结构：{@code {"cid":"...","uid":"...","last_read_mid":"...","last_read_time":...}}
     */
    public ObjectNode readStateUpdatedPayload(CPChannelReadState state) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cid", Long.toString(state.getCid()));
        payload.put("uid", Long.toString(state.getUid()));
        payload.put("last_read_mid", Long.toString(state.getLastReadMid()));
        payload.put("last_read_time", state.getLastReadTime());
        return payload;
    }

    /**
     * 构造 {@code channels.changed} 的 payload（提示刷新）。
     * <p>
     * 固定结构：{@code {"hint":"refresh"}}
     */
    public ObjectNode channelsChangedPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("hint", "refresh");
        return payload;
    }

    /**
     * 构造 {@code channel.changed} 的 payload（提示刷新）。
     * <p>
     * 固定结构：{@code {"cid":"...","scope":"...","hint":"refresh"}}
     */
    public ObjectNode channelChangedPayload(long cid, String scope) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cid", Long.toString(cid));
        payload.put("scope", scope == null ? "" : scope);
        payload.put("hint", "refresh");
        return payload;
    }

    /**
     * 将消息对象映射为 WS message item（用于 message.created）。
     * <p>
     * 关键约束：
     * <ul>
     *   <li>所有 ID 字段统一转为十进制字符串</li>
     *   <li>domain_version 为空时默认 {@code 1.0.0}</li>
     *   <li>reply_to_mid 仅在 >0 时输出</li>
     *   <li>preview 统一由 {@link ApiMessagePreviewService} 生成</li>
     * </ul>
     */
    private ObjectNode messageItem(CPMessage m) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("mid", Long.toString(m.getId()));
        msg.put("cid", Long.toString(m.getCid()));
        msg.put("uid", Long.toString(m.getUid()));
        msg.set("sender", senderOf(m.getUid()));
        msg.put("send_time", TimeUtil.localDateTimeToMillis(m.getSendTime()));
        msg.put("domain", m.getDomain());
        msg.put("domain_version", m.getDomainVersion() == null ? "1.0.0" : m.getDomainVersion());
        if (m.getReplyToMid() > 0) {
            msg.put("reply_to_mid", Long.toString(m.getReplyToMid()));
        }
        JsonNode data = m.getData();
        msg.set("data", data == null ? objectMapper.createObjectNode() : data);
        msg.put("preview", previewService.preview(m.getDomain(), data));
        return msg;
    }

    /**
     * 构造 sender 对象（uid/nickname/avatar）。
     */
    private ObjectNode senderOf(long uid) {
        CPUser user = userDao.getById(uid);
        ObjectNode sender = objectMapper.createObjectNode();
        sender.put("uid", Long.toString(uid));
        if (user == null) {
            sender.put("nickname", "");
            sender.put("avatar", "");
            return sender;
        }
        sender.put("nickname", user.getUsername() == null ? "" : user.getUsername());
        sender.put("avatar", avatarPath(user.getAvatar()));
        return sender;
    }

    /**
     * 将头像文件 ID 映射为下载相对路径。
     */
    private String avatarPath(long avatarId) {
        if (avatarId <= 0) {
            return "";
        }
        CPFileInfo info = fileInfoDao.getById(avatarId);
        if (info == null || info.getShareKey() == null || info.getShareKey().isBlank()) {
            return "";
        }
        return "api/files/download/" + info.getShareKey();
    }
}
