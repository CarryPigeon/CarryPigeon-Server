package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.service.preview.ApiMessagePreviewService;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 消息创建结果映射节点（HTTP：{@code POST /api/channels/{cid}/messages}）。
 *
 * <p>输入（上游落库节点写入）：
 * {@link CPNodeMessageKeys#MESSAGE_INFO} = {@link CPMessage}
 *
 * <p>输出：构造对外响应体（本节点直接返回给 HTTP Controller）。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiMessageCreateResult")
public class ApiMessageCreateResultNode extends AbstractResultNode<ApiMessageCreateResultNode.MessageItem> {

    /** 用户查询 DAO（构造 sender 信息）。 */
    private final UserDao userDao;
    /** 文件查询 DAO（解析头像 share_key）。 */
    private final FileInfoDao fileInfoDao;
    /** preview 生成服务（未装插件的降级展示）。 */
    private final ApiMessagePreviewService previewService;

    /**
     * 构造消息创建响应体。
     *
     * <p>依赖上下文：{@link CPNodeMessageKeys#MESSAGE_INFO}。
     *
     * <p>对外口径：雪花 ID 统一用十进制字符串，avatar/schema_url 等地址统一为相对路径（不带 host）。
     */
    @Override
    protected MessageItem build(CPFlowContext context) throws Exception {
        CPMessage message = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO);
        Sender sender = senderOf(message.getUid());
        MessageItem item = new MessageItem(
                Long.toString(message.getId()),
                Long.toString(message.getCid()),
                Long.toString(message.getUid()),
                sender,
                TimeUtil.localDateTimeToMillis(message.getSendTime()),
                message.getDomain(),
                message.getDomainVersion() == null ? "1.0.0" : message.getDomainVersion(),
                message.getReplyToMid() > 0 ? Long.toString(message.getReplyToMid()) : null,
                message.getData(),
                previewService.preview(message.getDomain(), message.getData())
        );
        log.debug("消息创建结果映射：完成：mid={}", message.getId());
        return item;
    }

    /**
     * 构造发送者信息（用于列表/详情展示）。
     */
    private Sender senderOf(long uid) {
        CPUser user = userDao.getById(uid);
        if (user == null) {
            return new Sender(Long.toString(uid), "", "");
        }
        return new Sender(Long.toString(uid), user.getUsername(), avatarPath(user.getAvatar()));
    }

    /**
     * 头像下载相对路径。
     *
     * <p>注意：返回的是相对路径（不含 host），由客户端拼接当前 server_host 使用。
     */
    private String avatarPath(long avatarId) {
        if (avatarId <= 0) {
            return "";
        }
        var info = fileInfoDao.getById(avatarId);
        if (info == null || info.getShareKey() == null || info.getShareKey().isBlank()) {
            return "";
        }
        return "api/files/download/" + info.getShareKey();
    }

    /**
     * 发送者展示信息。
     */
    public record Sender(String uid, String nickname, String avatar) {
    }

    /**
     * 消息创建响应 item（与 API 文档对齐：雪花 ID 使用十进制字符串）。
     */
    public record MessageItem(
            String mid,
            String cid,
            String uid,
            Sender sender,
            long sendTime,
            String domain,
            String domainVersion,
            String replyToMid,
            JsonNode data,
            String preview
    ) {
    }
}
