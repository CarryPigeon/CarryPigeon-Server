package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 获取用户信息结果Node<br/>
 * 入参：UserInfo:{@link CPUser}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserGetProfileResult")
public class CPUserGetProfileResultNode extends CPNodeComponent {

    private final ObjectMapper objectMapper;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        // 获取绑定数据
        CPUser userInfo = getBindData("UserInfo",CPUser.class);
        if (userInfo == null){
            argsError(context);
        }
        assert userInfo != null;
        Result result = new Result()
                .setUsername(userInfo.getUsername())
                .setAvatar(userInfo.getAvatar())
                .setEmail(userInfo.getEmail())
                .setSex(userInfo.getSex().getValue())
                .setBrief(userInfo.getBrief())
                .setBirthday(TimeUtil.LocalDateTimeToMillis(userInfo.getBirthday()));
        context.setData("response", CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    private static class Result{
        private String username;
        private long avatar;
        private String email;
        private int sex;
        private String brief;
        private long birthday;
    }
}
