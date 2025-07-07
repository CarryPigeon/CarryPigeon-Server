package team.carrypigeon.backend.chat.domain.pojo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 路径分发具体对象，详细请见
 * <a href="https://www.yuque.com/cpointerz/cp/ngfaxwn1vrpr121v">通用数据交换协议及简介</a>
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPRoute {
    private long id; // 请求id
    private String route; // 分发路径
    private JsonNode data; // 具体数据
}
