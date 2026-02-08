package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
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
    protected void process(CPFlowContext context) throws Exception {
        CPUser user = requireContext(context, CPNodeUserKeys.USER_INFO);
        String username = context.get(CPNodeUserKeys.USER_INFO_USER_NAME);
        if (username != null){
            user.setUsername(username);
        }
        String brief = context.get(CPNodeUserKeys.USER_INFO_BRIEF);
        if (brief != null){
            user.setBrief(brief);
        }
        Long avatar = context.get(CPNodeUserKeys.USER_INFO_AVATAR);
        if (avatar != null){
            user.setAvatar(avatar);
        }
        String email = context.get(CPNodeUserKeys.USER_INFO_EMAIL);
        if (email != null){
            user.setEmail(email);
        }
        Integer sex = context.get(CPNodeUserKeys.USER_INFO_SEX);
        if (sex != null){
            user.setSex(CPUserSexEnum.valueOf(sex));
        }
        Long birthday = context.get(CPNodeUserKeys.USER_INFO_BIRTHDAY);
        if (birthday != null){
            user.setBirthday(TimeUtil.millisToLocalDateTime(birthday));
        }

    }
}
