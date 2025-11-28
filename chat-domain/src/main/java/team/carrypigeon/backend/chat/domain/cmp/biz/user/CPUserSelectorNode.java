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
 * 鐢ㄤ簬閫氳繃鐢ㄦ埛id鑾峰彇鏁版嵁搴撶粨鏋勭殑selector<br/>
 * 鏌ヨ鐨勬ā鏉挎湁閫氳繃id鏌ヨ涓庨€氳繃閭鏌ヨ锛屽湪浣跨敤鏃跺簲璇ョ粰key鍒嗗埆缁戝畾id涓巈mail<br/>
 * 1. id鏌ヨ鐨勫叆鍙傦細UserInfo_Id:Long<br/>
 * 2. email鏌ヨ鐨勫叆鍙傦細UserInfo_Email:String<br/>
 * 3. 榛樿鏌ヨ鍒欓€氳繃鏃ュ織鎶ラ敊 TODO <br/>
 * 鍑哄弬: UserInfo:CPUser<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserSelector")
public class CPUserSelectorNode extends CPNodeComponent {

    private final UserDao userDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        // 鑾峰彇缁戝畾鏁版嵁
        String key = getBindData("key",String.class);
        if (key == null){
            argsError(context);
        }
        // 閫氳繃缁戝畾鏁版嵁鑾峰彇鏁版嵁
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
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("user not found"));
            throw new CPReturnException();
        }
        // 鍙傛暟鏀惧叆涓婁笅鏂?        context.setData(CPNodeValueKeyBasicConstants.USER_INFO, userInfo);
    }
}

