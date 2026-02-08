package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用户token刷新节点<br/>
 * 入参: UserToken:{@link CPUserToken}<br/>
 * 出参: UserToken:{@link CPUserToken}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenRefresh")
public class CPUserTokenRefreshNode extends CPNodeComponent {
    @Override
    protected void process(CPFlowContext context) throws Exception {
        CPUserToken userToken = requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO);
        userToken.setExpiredTime(TimeUtil.currentLocalDateTime().plusDays(30));
        userToken.setToken(IdUtil.generateToken());
    }
}
