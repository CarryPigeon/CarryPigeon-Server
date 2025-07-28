package team.carrypigeon.backend.dao.impl.channel;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.channel.CPChatStructureTypeDAO;
import team.carrypigeon.backend.api.bo.domain.channel.ChatStructureTypeBO;
import team.carrypigeon.backend.api.bo.domain.channel.ChatStructureTypeMenu;
import team.carrypigeon.backend.dao.mapper.channel.ChannelTypeMapper;
import team.carrypigeon.backend.dao.mapper.channel.ChannelTypePO;

@Component
public class CPChannelTypeImpl implements CPChatStructureTypeDAO {

    private final ChannelTypeMapper channelTypeMapper;

    public CPChannelTypeImpl(ChannelTypeMapper channelTypeMapper) {
        this.channelTypeMapper = channelTypeMapper;
    }

    @Override
    public ChatStructureTypeBO getChatStructureType(long chatId) {
        QueryWrapper<ChannelTypePO> wrapper = new QueryWrapper<ChannelTypePO>().eq("channel_id", chatId);
        ChannelTypePO channelTypePO = channelTypeMapper.selectOne(wrapper);
        if (channelTypePO == null) return null;
        ChatStructureTypeBO channelTypeBO = new ChatStructureTypeBO();
        String[] split = channelTypePO.getType().split(":");
        channelTypeBO.setType(ChatStructureTypeMenu.valueOfByName(split[0]));
        channelTypeBO.setTypeName(channelTypePO.getType().substring(split[0].length()+1));
        return channelTypeBO;
    }
}
