package team.carrypigeon.backend.api.bo.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * PLUGIN->plugin:pluginName
     * */
    public String toDomain() {
        return switch (type) {
            case CORE -> "core";
            case PLUGIN -> "plugin:"+pluginName;
        };
    }

    /**
     * 用于通过数据库中存储的结构获取CPMessageDomain
     * */
    public static CPMessageDomain fromDomain(String domain) {
        if (domain.equals("core")) {
            return new CPMessageDomain(CPMessageDomainEnum.CORE);
        }else {
            return new CPMessageDomain(CPMessageDomainEnum.PLUGIN, domain.substring(domain.indexOf(":")+1));
        }
    }
}
