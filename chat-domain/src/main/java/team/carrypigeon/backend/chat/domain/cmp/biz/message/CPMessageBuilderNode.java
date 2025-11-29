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
 * 将 {@link CPMessageData} 组装为可持久化的 {@link CPMessage} 实体。<br/>
 * 输入：<br/>
 *  - MessageData:{@link CPMessageData}<br/>
 *  - ChannelInfo_Id:Long  消息所属频道 id<br/>
 *  - ChannelMemberInfo_Uid:Long  发送消息的用户 id<br/>
 * 输出：<br/>
 *  - MessageInfo:{@link CPMessage}  构建完成的消息实体
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPMessageBuilder")
public class CPMessageBuilderNode extends CPNodeComponent {

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
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
