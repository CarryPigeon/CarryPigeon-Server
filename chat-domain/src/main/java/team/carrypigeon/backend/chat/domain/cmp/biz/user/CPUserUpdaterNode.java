package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用户更新信息节点<br/>
 * 入参:<br/>
 * 1. UserInfo:{@link CPUser}<br/>
 * 2. UserInfo_UserName:String?<br/>
 * 3. UserInfo_Brief:String?<br/>
 * 4. UserInfo_Avatar:Long?<br/>
 * 5. UserInfo_Email:String?<br/>
 * 6. UserInfo_Sex:Integer?<br/>
 * 7. UserInfo_Birthday:Long?<br/>
 * 出参：无
 * @author midreamsheep
 * */
@LiteflowComponent("CPUserUpdater")
public class CPUserUpdaterNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUser user = context.getData("UserInfo");
        if (user == null){
            argsError(context);
        }
        assert user != null;
        String username = context.getData("UserInfo_UserName");
        if (username != null){
            user.setUsername(username);
        }
        String brief = context.getData("UserInfo_Brief");
        if (brief != null){
            user.setBrief(brief);
        }
        Long avatar = context.getData("UserInfo_Avatar");
        if (avatar != null){
            user.setAvatar(avatar);
        }
        String email = context.getData("UserInfo_Email");
        if (email != null){
            user.setEmail(email);
        }
        Integer sex = context.getData("UserInfo_Sex");
        if (sex != null){
            user.setSex(CPUserSexEnum.valueOf(sex));
        }
        Long birthday = context.getData("UserInfo_Birthday");
        if (birthday != null){
            user.setBirthday(TimeUtil.MillisToLocalDateTime(birthday));
        }

    }
}
