package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用于构建用户信息的Node<br/>
 * 入参：<br/>
 * 1. UserInfo_Id:Long<br/>
 * 2. UserInfo_UserName:String<br/>
 * 3. UserInfo_Brief:String<br/>
 * 4. UserInfo_Avatar:Long<br/>
 * 5. UserInfo_Email:String<br/>
 * 6. UserInfo_Sex:Integer<br/>
 * 7. UserInfo_Birthday:Long<br/>
 * 8. UserInfo_RegisterTime:Long<br/>
 * 出参: UserInfo:{@link CPUser}<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("CPUserBuilder")
public class CPUserBuilderNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long userInfoId = context.getData("UserInfo_Id");
        String userInfoUserName = context.getData("UserInfo_UserName");
        String userInfoBrief = context.getData("UserInfo_Brief");
        Long userInfoAvatar = context.getData("UserInfo_Avatar");
        String userInfoEmail = context.getData("UserInfo_Email");
        Integer userInfoSex = context.getData("UserInfo_Sex");
        Long userInfoBirthday = context.getData("UserInfo_Birthday");
        Long userInfoRegisterTime = context.getData("UserInfo_RegisterTime");
        if (userInfoId == null || userInfoUserName == null || userInfoBrief == null || userInfoAvatar == null || userInfoEmail == null || userInfoSex == null || userInfoBirthday == null || userInfoRegisterTime == null){
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
        context.setData("UserInfo",cpUser);
    }
}
