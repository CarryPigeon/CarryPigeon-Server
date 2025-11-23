package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.CPReturnException;

/**
 * 用于通过用户id获取数据库结构的selector<br/>
 * 查询的模板有通过id查询与通过邮箱查询，在使用时应该给key分别绑定id与email<br/>
 * 1. id查询的入参：UserInfo_Id:Long<br/>
 * 2. email查询的入参：UserInfo_Email:String<br/>
 * 3. 默认查询则通过日志报错 TODO <br/>
 * 出参: UserInfo:CPUser<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserSelector")
public class CPUserSelectorNode extends CPNodeComponent {

    private final UserDao userDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        // 获取绑定数据
        String key = getBindData("key",String.class);
        if (key == null){
            argsError(context);
        }
        // 通过绑定数据获取数据
        CPUser userInfo = null;
        switch (key){
            case "id":
                Long id = getBindData("UserInfo_Id",Long.class);
                if (id == null){
                    argsError(context);
                }
                userInfo = userDao.getById(id);
                break;
            case "email":
                String email = getBindData("UserInfo_Email",String.class);
                if (email == null){
                    argsError(context);
                }
                userInfo = userDao.getByEmail(email);
                break;
            case null:
            default:
                argsError(context);
                break;
        }
        if (userInfo == null){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("user not found"));
            throw new CPReturnException();
        }
        // 参数放入上下文
        context.setData("UserInfo",userInfo);
    }
}