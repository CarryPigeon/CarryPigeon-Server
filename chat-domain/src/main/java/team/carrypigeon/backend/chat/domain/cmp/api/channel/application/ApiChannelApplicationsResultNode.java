package team.carrypigeon.backend.chat.domain.cmp.api.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 入群申请列表结果节点。
 * <p>
 * 将申请记录集合映射为 API 响应列表。
 */
@Slf4j
@LiteflowComponent("ApiChannelApplicationsResult")
public class ApiChannelApplicationsResultNode extends AbstractResultNode<ApiChannelApplicationsResultNode.ApplicationsResponse> {

    /**
     * 构建申请列表响应。
     */
    @Override
    protected ApplicationsResponse build(CPFlowContext context) {
        @SuppressWarnings("unchecked")
        Set<CPChannelApplication> apps = (Set<CPChannelApplication>) requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_LIST);

        List<ApplicationItem> items = apps.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(a -> a.getId() == null ? 0L : a.getId()))
                .map(a -> new ApplicationItem(
                        Long.toString(a.getId()),
                        Long.toString(a.getCid()),
                        Long.toString(a.getUid()),
                        a.getMsg() == null ? "" : a.getMsg(),
                        a.getApplyTime() == null ? 0L : TimeUtil.localDateTimeToMillis(a.getApplyTime()),
                        statusOf(a.getState())
                ))
                .toList();

        ApplicationsResponse resp = new ApplicationsResponse(items);
        log.debug("ApiChannelApplicationsResult success, size={}", items.size());
        return resp;
    }

    /**
     * 将申请状态枚举映射为对外状态字符串。
     */
    private String statusOf(CPChannelApplicationStateEnum state) {
        if (state == null) {
            return "pending";
        }
        return switch (state) {
            case PENDING -> "pending";
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
        };
    }

    /**
     * 申请列表响应体。
     */
    public record ApplicationsResponse(List<ApplicationItem> items) {
    }

    /**
     * 单个申请响应项。
     */
    public record ApplicationItem(String applicationId, String cid, String uid, String reason, long applyTime, String status) {
    }
}
