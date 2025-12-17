package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * Select a valid channel ban record by channel id and target uid.
 * Input: ChannelInfo_Id(Long), ChannelBan_TargetUid(Long).
 * Output: ChannelBanInfo(CPChannelBan).
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelBanSelector")
public class CPChannelBanSelectorNode extends AbstractSelectorNode<CPChannelBan> {

    private final ChannelBanDAO channelBanDAO;

    @Override
    protected String readMode(CPFlowContext context) {
        // 不依赖 bind，固定查询当前频道 + 用户的封禁记录
        return "default";
    }

    @Override
    protected CPChannelBan doSelect(String mode, CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID, Long.class);
        Long targetUid = requireContext(context, CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID, Long.class);
        CPChannelBan ban = select(context,
                buildSelectKey("channel_ban", java.util.Map.of("cid", cid, "uid", targetUid)),
                () -> channelBanDAO.getByChannelIdAndUserId(targetUid, cid));
        if (ban == null || !ban.isValid()) {
            return null;
        }
        return ban;
    }

    @Override
    protected String getResultKey() {
        return CPNodeChannelBanKeys.CHANNEL_BAN_INFO;
    }

    @Override
    protected void handleNotFound(String mode, CPFlowContext context) throws CPReturnException {
        log.info("CPChannelBanSelector: ban not found or expired");
        businessError(context, "this user is not banned");
    }

    @Override
    protected void afterSuccess(String mode, CPChannelBan entity, CPFlowContext context) {
        log.debug("CPChannelBanSelector success, banId={}, cid={}, uid={}",
                entity.getId(), entity.getCid(), entity.getUid());
    }
}
