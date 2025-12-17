package team.carrypigeon.backend.api.chat.domain.controller;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 所有controller的VO接口<br/>
 * 用于处理将数据转换到context中且进行第一步的参数存在性校验与范围校验<br/>
 * @author midreamsheep
 * */
public interface CPControllerVO {
    /**
     * 将数据转换到context中，用于将json中的data字段转换为可被liteflow的chain具体处理的单元数据<be/>
     * @param context liteflow上下文
     * @return 是否转换成功，不成功的情况可能为数据不合法获取缺少数据
     * */
    boolean insertData(CPFlowContext context);
}
