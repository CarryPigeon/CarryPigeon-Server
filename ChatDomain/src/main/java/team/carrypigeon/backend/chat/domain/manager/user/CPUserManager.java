package team.carrypigeon.backend.chat.domain.manager.user;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.domain.CPChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class CPUserManager {
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
    }
}
