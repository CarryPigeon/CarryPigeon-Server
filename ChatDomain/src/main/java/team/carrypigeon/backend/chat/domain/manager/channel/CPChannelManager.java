package team.carrypigeon.backend.chat.domain.manager.channel;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.CPChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用户id到通道列表的映射
 * */
@Component
public class CPChannelManager {
    // 使用ConcurrentHashMap保证外部Map的线程安全
    private final ConcurrentHashMap<Long, List<CPChannel>> map = new ConcurrentHashMap<>();

    public List<CPChannel> getChannels(Long userId) {
        // computeIfAbsent是原子操作，线程安全
        return map.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
    }

    public void removeChannel(CPChannel channel) {
        Long userId = channel.getCPUserBO().getId();
        List<CPChannel> channels = map.get(userId);
        if (channels != null) {
            channels.remove(channel);
        }
    }

    public void addChannel(CPChannel channel) {
        // CopyOnWriteArrayList的add操作是线程安全的
        getChannels(channel.getCPUserBO().getId()).add(channel);
    }

    // 批量操作方法
    public void addChannels(Long userId, List<CPChannel> channels) {
        map.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).addAll(channels);
    }

    public List<CPChannel> getChannelsSnapshot(Long userId) {
        List<CPChannel> channels = map.get(userId);
        if (channels != null) {
            return new ArrayList<>(channels); // 返回快照
        }
        return new ArrayList<>();
    }

    public boolean containsUser(Long userId) {
        return map.containsKey(userId);
    }

    public int getTotalUserCount() {
        return map.size();
    }

    public int getTotalChannelCount() {
        return map.values().stream().mapToInt(List::size).sum();
    }

    public void removeUser(Long userId) {
        map.remove(userId);
    }

    public void clear() {
        map.clear();
    }
}
