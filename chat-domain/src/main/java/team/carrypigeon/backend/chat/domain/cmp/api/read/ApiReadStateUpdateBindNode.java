package team.carrypigeon.backend.chat.domain.cmp.api.read;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ReadStateUpdateRequest;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.List;
import java.util.Map;

/**
 * 已读状态更新请求绑定节点。
 * <p>
 * 解析已读更新时间请求并写入频道/用户/时间上下文字段。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("ApiReadStateUpdateBind")
public class ApiReadStateUpdateBindNode extends CPNodeComponent {

    private final ChannelMessageDao channelMessageDao;

    /**
     * 解析并绑定已读状态更新请求。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ReadStateUpdateRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        long lastReadMid = parseId(req.lastReadMid(), "last_read_mid");

        CPMessage message = channelMessageDao.getById(lastReadMid);
        if (message == null || message.getCid() != cid) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "last_read_mid", "reason", "invalid", "message", "message not found in channel")
                    ))));
        }
        long lastReadTime = TimeUtil.localDateTimeToMillis(message.getSendTime());

        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, cid);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_MID, lastReadMid);
        context.set(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, lastReadTime);
        log.debug("ApiReadStateUpdateBind success, cid={}, lastReadMid={}, lastReadTime={}", cid, lastReadMid, lastReadTime);
    }

    /**
     * 解析字符串形式的 ID。
     */
    private long parseId(String str, String field) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }
}
