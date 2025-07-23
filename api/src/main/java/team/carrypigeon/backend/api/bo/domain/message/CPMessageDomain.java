package team.carrypigeon.backend.api.bo.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 消息域，用于标识消息是核心消息还是插件的自定义消息
 * 核心消息仅需标识为CORE，插件消息需要额外提供插件名数据
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPMessageDomain {
    private CPMessageDomainEnum type;
    private String pluginName;

    public CPMessageDomain(CPMessageDomainEnum cpMessageDomainEnum) {
        this.type = cpMessageDomainEnum;
    }

    /**
     * 用于输出为数据库中存储的结构
     * CORE->core
     * PLUGIN->plugins:pluginName
     * */
    @Override
    public String toString() {
        return switch (type) {
            case CORE -> "core";
            case PLUGINS -> "plugins:"+pluginName;
        };
    }
}
