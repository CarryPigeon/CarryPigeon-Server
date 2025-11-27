package team.carrypigeon.backend.chat.domain.cmp.basic;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;

/**
 * 重命名参数节点<br/>
 * 需要重命名的参数必须以;隔开，例如：CurId:UserInfo_Id;Email:UserInfo_Email<br/>
 * 注：修改参数只会新增引用，不会删除原参数<br/>
 * @author midreamsheep
 * */
@LiteflowComponent("RenameArg")
public class RenameArgNode extends CPNodeComponent {

    /**
     * LiteFlow 中 bind 使用的参数名。<br/>
     * 约定所有 Node.bind 的第一个参数均为 "key"。
     */
    private static final String BIND_KEY = "key";

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        // 从组件配置中读取重命名规则，示例："CurId:UserInfo_Id;Email:UserInfo_Email"
        String bindData = this.getBindData(BIND_KEY, String.class);
        if (bindData == null) {
            argsError(context);
            return;
        }
        String[] renames = bindData.split(";");
        for (String s : renames) {
            String[] map = s.split(":");
            if (map.length != 2) {
                continue;
            }
            context.setData(map[0], context.getData(map[1]));
        }
    }
}
