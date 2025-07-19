package team.carrypigeon.backend.dao.impl.channel;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.channel.CPChannelTypeDAO;
import team.carrypigeon.backend.api.bo.domain.channel.ChannelTypeBO;
import team.carrypigeon.backend.api.bo.domain.channel.ChannelTypeMenu;
import team.carrypigeon.backend.dao.mapper.channel.ChannelTypeMapper;
import team.carrypigeon.backend.dao.mapper.channel.ChannelTypePO;

@Component
public class CPChannelTypeImpl implements CPChannelTypeDAO {

    private final ChannelTypeMapper channelTypeMapper;

    public CPChannelTypeImpl(ChannelTypeMapper channelTypeMapper) {
        this.channelTypeMapper = channelTypeMapper;
    }

    @Override
    public ChannelTypeBO getChannelType(long channelId) {
        QueryWrapper<ChannelTypePO> wrapper = new QueryWrapper<ChannelTypePO>().eq("channel_id", channelId);
        ChannelTypePO channelTypePO = channelTypeMapper.selectOne(wrapper);
        if (channelTypePO == null) return null;
        ChannelTypeBO channelTypeBO = new ChannelTypeBO();
        String[] split = channelTypePO.getType().split(":");
        System.out.println(split[0]);
        channelTypeBO.setType(ChannelTypeMenu.valueOfByName(split[0]));
        channelTypeBO.setTypeName(channelTypePO.getType().substring(split[0].length()+1));
        System.out.println(channelTypeBO);
        return channelTypeBO;
    }
}
