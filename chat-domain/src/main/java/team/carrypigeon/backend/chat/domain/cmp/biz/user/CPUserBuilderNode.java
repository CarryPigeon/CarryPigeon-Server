package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用于根据上下文中的 UserInfo_* 字段构建 {@link CPUser} 实例的节点。<br/>
 * 入参：<br/>
 * 1. UserInfo_Id:Long<br/>
 * 2. UserInfo_UserName:String<br/>
 * 3. UserInfo_Brief:String<br/>
 * 4. UserInfo_Avatar:Long<br/>
 * 5. UserInfo_Email:String<br/>
 * 6. UserInfo_Sex:Integer<br/>
 * 7. UserInfo_Birthday:Long<br/>
 * 8. UserInfo_RegisterTime:Long<br/>
 * 出参：UserInfo:{@link CPUser}<br/>
 */
@LiteflowComponent("CPUserBuilder")
public class CPUserBuilderNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long userInfoId = requireContext(context, CPNodeUserKeys.USER_INFO_ID, Long.class);
        String userInfoUserName = requireContext(context, CPNodeUserKeys.USER_INFO_USER_NAME, String.class);
        String userInfoBrief = requireContext(context, CPNodeUserKeys.USER_INFO_BRIEF, String.class);
        Long userInfoAvatar = requireContext(context, CPNodeUserKeys.USER_INFO_AVATAR, Long.class);
        String userInfoEmail = requireContext(context, CPNodeUserKeys.USER_INFO_EMAIL, String.class);
        Integer userInfoSex = requireContext(context, CPNodeUserKeys.USER_INFO_SEX, Integer.class);
        Long userInfoBirthday = requireContext(context, CPNodeUserKeys.USER_INFO_BIRTHDAY, Long.class);
        Long userInfoRegisterTime = requireContext(context, CPNodeUserKeys.USER_INFO_REGISTER_TIME, Long.class);
        CPUser cpUser = new CPUser();
        cpUser.setId(userInfoId)
                .setUsername(userInfoUserName)
                .setBrief(userInfoBrief)
                .setAvatar(userInfoAvatar)
                .setEmail(userInfoEmail)
                .setSex(CPUserSexEnum.valueOf(userInfoSex))
                .setBirthday(TimeUtil.MillisToLocalDateTime(userInfoBirthday))
                .setRegisterTime(TimeUtil.MillisToLocalDateTime(userInfoRegisterTime));
        context.setData(CPNodeUserKeys.USER_INFO, cpUser);
    }
}
