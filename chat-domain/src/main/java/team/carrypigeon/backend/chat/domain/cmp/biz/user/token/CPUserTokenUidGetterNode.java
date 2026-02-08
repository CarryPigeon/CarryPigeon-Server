package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

/**
 * 获取用户token的uid<br/>
 * 入参: UserToken:{@link CPUserToken}<br/>
 * 出参: UserInfo_Id:{@link Long}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenUidGetter")
public class CPUserTokenUidGetterNode extends CPNodeComponent {
    @Override
    protected void process(CPFlowContext context) throws Exception {
        CPUserToken userToken = requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO);
        context.set(CPNodeUserKeys.USER_INFO_ID, userToken.getUid());
    }
}
