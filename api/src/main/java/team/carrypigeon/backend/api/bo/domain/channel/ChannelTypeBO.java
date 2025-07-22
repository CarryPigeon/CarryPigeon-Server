package team.carrypigeon.backend.api.bo.domain.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天结构的bo模型，用于标识当前聊天组是内置类型还是插件类型
 * 为两级标注，type标注为内置类型还是插件类型，typeName标注具体类型名称
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelTypeBO {
    private ChannelTypeMenu type;
    private String typeName;
}
