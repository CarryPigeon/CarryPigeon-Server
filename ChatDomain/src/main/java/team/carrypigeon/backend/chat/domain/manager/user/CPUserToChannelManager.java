package team.carrypigeon.backend.chat.domain.manager.user;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.CPChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 用户id到通道列表的映射
 * */
@Component
public class CPUserToChannelManager {
    private final HashMap<Long, List<CPChannel>> map = new HashMap<>();

    public List<CPChannel> getChannels(Long userId) {
        if(!map.containsKey(userId)){
            map.put(userId,new ArrayList<>());
        }
        return map.get(userId);
    }

    public void removeChannel(CPChannel channel) {
        map.get(channel.getCPUserBO().getId()).remove(channel);
    }

    public void addChannel(CPChannel channel) {
        getChannels(channel.getCPUserBO().getId()).add(channel);
        System.out.println(channel.getCPUserBO().getId());
    }
}
