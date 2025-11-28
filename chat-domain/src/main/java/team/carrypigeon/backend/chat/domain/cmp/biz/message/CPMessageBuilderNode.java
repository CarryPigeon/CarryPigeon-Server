package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * ?????? {@link CPMessageData} ??????? {@link CPMessage}?
 * ???
 *  - MessageData:{@link CPMessageData}
 *  - ChannelInfo_Id:Long
 *  - ChannelMemberInfo_Uid:Long
 * ???
 *  - MessageInfo:{@link CPMessage}
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageBuilder")
public class CPMessageBuilderNode extends CPNodeComponent {

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPMessageData messageData = context.getData(CPNodeValueKeyExtraConstants.MESSAGE_DATA);
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        Long uid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_UID);
        if (messageData == null || cid == null || uid == null) {
            log.error("CPMessageBuilder args error, messageData={}, cid={}, uid={}",
                    messageData, cid, uid);
            argsError(context);
            return;
        }
        CPMessage message = new CPMessage()
                .setId(IdUtil.generateId())
                .setCid(cid)
                .setUid(uid)
                .setDomain(messageData.getDomain())
                .setData(messageData.getData())
                .setSendTime(TimeUtil.getCurrentLocalTime());
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO, message);
        log.info("CPMessageBuilder success, mid={}, cid={}, uid={}, domain={}",
                message.getId(), message.getCid(), message.getUid(), message.getDomain());
    }
}
