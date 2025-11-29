package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
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
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long userInfoId = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_ID);
        String userInfoUserName = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_USER_NAME);
        String userInfoBrief = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_BRIEF);
        Long userInfoAvatar = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_AVATAR);
        String userInfoEmail = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_EMAIL);
        Integer userInfoSex = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_SEX);
        Long userInfoBirthday = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_BIRTHDAY);
        Long userInfoRegisterTime = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_REGISTER_TIME);
        if (userInfoId == null || userInfoUserName == null || userInfoBrief == null
                || userInfoAvatar == null || userInfoEmail == null
                || userInfoSex == null || userInfoBirthday == null || userInfoRegisterTime == null) {
            argsError(context);
        }
        CPUser cpUser = new CPUser();
        cpUser.setId(userInfoId)
                .setUsername(userInfoUserName)
                .setBrief(userInfoBrief)
                .setAvatar(userInfoAvatar)
                .setEmail(userInfoEmail)
                .setSex(CPUserSexEnum.valueOf(userInfoSex))
                .setBirthday(TimeUtil.MillisToLocalDateTime(userInfoBirthday))
                .setRegisterTime(TimeUtil.MillisToLocalDateTime(userInfoRegisterTime));
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO, cpUser);
    }
}
