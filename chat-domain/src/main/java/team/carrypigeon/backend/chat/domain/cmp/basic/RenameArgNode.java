package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;

/**
 * 重命名参数节点<br/>
 * 需要重命名的参数必须以;隔开，例如：CurId:UserInfo_Id;Email:UserInfo_Email<br/>
 * 注：修改参数只会新增引用，不会删除原参数<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("RenameArg")
public class RenameArgNode extends CPNodeComponent {

    private static final String RENAME_ARG_KEY = "RenameArg";

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String bindData = this.getBindData(RENAME_ARG_KEY, String.class);
        if (bindData == null){
            argsError(context);
        }
        assert bindData != null;
        String[] renames = bindData.split(";");
        for (String s : renames) {
            String[] map = s.split(":");
            context.setData(map[0], context.getData(map[1]));
        }
    }
}
