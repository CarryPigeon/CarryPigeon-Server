package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.service.preview.ApiMessagePreviewService;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息列表结果映射节点（HTTP：{@code GET /api/channels/{cid}/messages}）。
 *
 * <p>输入（上游查询节点写入）：
 * <ul>
 *   <li>{@link CPNodeMessageKeys#MESSAGE_LIST}: {@code CPMessage[]}</li>
 *   <li>{@link CPNodeMessageKeys#MESSAGE_LIST_COUNT}: 请求 limit（Integer）</li>
 * </ul>
 *
 * <p>输出：对外响应（items + next_cursor + has_more）。
 *
 * <p>游标口径：{@code next_cursor} 使用“最后一条消息的 mid（雪花 ID）”，保证稳定分页。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiMessageListResult")
public class ApiMessageListResultNode extends AbstractResultNode<ApiMessageListResultNode.MessageListResponse> {

    /** 用户查询 DAO（批量构造 sender 信息）。 */
    private final UserDao userDao;
    /** 文件查询 DAO（批量解析头像 share_key）。 */
    private final FileInfoDao fileInfoDao;
    /** preview 生成服务（未装插件的降级展示）。 */
    private final ApiMessagePreviewService previewService;

    /**
     * 构造消息列表响应体。
     *
     * <p>依赖上下文：
     * <ul>
     *   <li>{@link CPNodeMessageKeys#MESSAGE_LIST}</li>
     *   <li>{@link CPNodeMessageKeys#MESSAGE_LIST_COUNT}（可选）</li>
     * </ul>
     *
     * <p>游标口径：当本次返回数达到/超过 limit 时，生成 {@code next_cursor}（取最后一条消息 mid）。
     */
    @Override
    protected MessageListResponse build(CPFlowContext context) throws Exception {
        CPMessage[] messages = requireContext(context, CPNodeMessageKeys.MESSAGE_LIST);
        Integer limit = context.get(CPNodeMessageKeys.MESSAGE_LIST_COUNT);
        int safeLimit = limit == null ? messages.length : limit;

        List<Long> uids = Arrays.stream(messages)
                .filter(Objects::nonNull)
                .map(CPMessage::getUid)
                .distinct()
                .toList();
        Map<Long, CPUser> usersByUid = new HashMap<>();
        for (CPUser u : userDao.listByIds(uids)) {
            if (u != null) {
                usersByUid.put(u.getId(), u);
            }
        }

        List<Long> avatarIds = usersByUid.values().stream()
                .map(CPUser::getAvatar)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        Map<Long, String> avatarShareKeys = fileInfoDao.listByIds(avatarIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CPFileInfo::getId, f -> f.getShareKey() == null ? "" : f.getShareKey(), (a, b) -> a));

        List<MessageItem> items = Arrays.stream(messages)
                .map(m -> new MessageItem(
                        Long.toString(m.getId()),
                        Long.toString(m.getCid()),
                        Long.toString(m.getUid()),
                        senderOf(m.getUid(), usersByUid, avatarShareKeys),
                        TimeUtil.localDateTimeToMillis(m.getSendTime()),
                        m.getDomain(),
                        m.getDomainVersion() == null ? "1.0.0" : m.getDomainVersion(),
                        m.getReplyToMid() > 0 ? Long.toString(m.getReplyToMid()) : null,
                        m.getData(),
                        previewService.preview(m.getDomain(), m.getData())
                ))
                .toList();

        String nextCursor = null;
        boolean hasMore = false;
        if (!items.isEmpty() && messages.length >= safeLimit) {
            hasMore = true;
            long lastMid = messages[messages.length - 1].getId();
            nextCursor = Long.toString(lastMid);
        }

        MessageListResponse response = new MessageListResponse(items, nextCursor, hasMore);
        log.debug("消息列表结果映射：完成：count={}, hasMore={}", items.size(), hasMore);
        return response;
    }

    /**
     * 构造发送者信息（带头像相对路径）。
     */
    private Sender senderOf(long uid, Map<Long, CPUser> usersByUid, Map<Long, String> avatarShareKeys) {
        CPUser user = usersByUid.get(uid);
        if (user == null) {
            return new Sender(Long.toString(uid), "", "");
        }
        return new Sender(Long.toString(uid), user.getUsername(), avatarPath(user.getAvatar(), avatarShareKeys));
    }

    /**
     * 头像下载相对路径。
     */
    private String avatarPath(long avatarId, Map<Long, String> avatarShareKeys) {
        if (avatarId <= 0) {
            return "";
        }
        String shareKey = avatarShareKeys.get(avatarId);
        if (shareKey == null || shareKey.isBlank()) {
            return "";
        }
        return "api/files/download/" + shareKey;
    }

    /**
     * 消息列表响应体。
     */
    public record MessageListResponse(List<MessageItem> items, String nextCursor, boolean hasMore) {
    }

    /**
     * 发送者展示信息。
     */
    public record Sender(String uid, String nickname, String avatar) {
    }

    /**
     * 列表中的消息 item（与 API 文档对齐：雪花 ID 使用十进制字符串）。
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
