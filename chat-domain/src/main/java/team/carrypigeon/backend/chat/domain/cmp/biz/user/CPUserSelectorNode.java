package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 根据用户 id 或邮箱从数据库中查询用户信息的 Selector 节点。<br/>
 * 查询模式通过 bind 参数 key 指定："id" / "email"。<br/>
 * 1. id 查询入参：UserInfo_Id:Long<br/>
 * 2. email 查询入参：UserInfo_Email:String<br/>
 * 出参：UserInfo:{@link CPUser}<br/>
 */
@AllArgsConstructor
@LiteflowComponent("CPUserSelector")
public class CPUserSelectorNode extends CPNodeComponent {

    private final UserDao userDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        // 从绑定参数中读取查询 key
        String key = getBindData("key", String.class);
        if (key == null) {
            argsError(context);
        }
        // 根据 key 从 DAO 查询用户信息
        CPUser userInfo = null;
        switch (key) {
            case "id":
                Long id = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_ID);
                if (id == null) {
                    argsError(context);
                }
                userInfo = userDao.getById(id);
                break;
            case "email":
                String email = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_EMAIL);
                if (email == null) {
                    argsError(context);
                }
                userInfo = userDao.getByEmail(email);
                break;
            case null:
            default:
                argsError(context);
                break;
        }
        if (userInfo == null) {
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("user not found"));
            throw new CPReturnException();
        }
        // 将查询到的用户写入上下文
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO, userInfo);
    }
}
