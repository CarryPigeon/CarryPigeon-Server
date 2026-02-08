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
 * Result mapper for {@code GET /api/channels/{cid}/applications}.
 */
@Slf4j
@LiteflowComponent("ApiChannelApplicationsResult")
public class ApiChannelApplicationsResultNode extends AbstractResultNode<ApiChannelApplicationsResultNode.ApplicationsResponse> {

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

    public record ApplicationsResponse(List<ApplicationItem> items) {
    }

    public record ApplicationItem(String applicationId, String cid, String uid, String reason, long applyTime, String status) {
    }
}

