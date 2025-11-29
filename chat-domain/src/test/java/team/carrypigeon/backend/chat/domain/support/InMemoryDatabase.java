package team.carrypigeon.backend.chat.domain.support;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Very small in-memory "database" used by test DAO implementations.
 * This allows controller/flow tests to exercise DAO calls without
 * requiring a real database.
 */
@Component
public class InMemoryDatabase {

    private final AtomicLong idGenerator = new AtomicLong(1L);

    private final Map<Long, CPUser> users = new HashMap<>();
    private final Map<Long, CPUserToken> userTokens = new HashMap<>();
    private final Map<String, CPUserToken> userTokensByToken = new HashMap<>();

    private final Map<Long, CPChannel> channels = new HashMap<>();

    private final Map<Long, CPChannelMember> channelMembers = new HashMap<>();
    // uid -> members
    private final Map<Long, List<CPChannelMember>> channelMembersByUid = new HashMap<>();
    // (uid,cid) -> member
    private final Map<String, CPChannelMember> channelMembersByUidCid = new HashMap<>();

    private final Map<Long, CPChannelApplication> channelApplications = new HashMap<>();

    private final Map<Long, CPChannelBan> channelBans = new HashMap<>();
    // (uid,cid) -> ban
    private final Map<String, CPChannelBan> channelBansByUidCid = new HashMap<>();

    private final Map<Long, CPMessage> messages = new HashMap<>();

    public long nextId() {
        return idGenerator.getAndIncrement();
    }

    // -------- User --------

    public void saveUser(CPUser user) {
        if (user.getId() == 0L) {
            user.setId(nextId());
        }
        users.put(user.getId(), user);
    }

    public CPUser getUserById(long id) {
        return users.get(id);
    }

    public CPUser getUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        return users.values().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    // -------- UserToken --------

    public void saveUserToken(CPUserToken token) {
        if (token.getId() == 0L) {
            token.setId(nextId());
        }
        userTokens.put(token.getId(), token);
        if (token.getToken() != null) {
            userTokensByToken.put(token.getToken(), token);
        }
    }

    public CPUserToken getUserTokenById(long id) {
        return userTokens.get(id);
    }

    public CPUserToken[] getUserTokensByUserId(long uid) {
        return userTokens.values().stream()
                .filter(t -> t.getUid() == uid)
                .toArray(CPUserToken[]::new);
    }

    public CPUserToken getUserTokenByToken(String token) {
        return userTokensByToken.get(token);
    }

    public boolean deleteUserToken(CPUserToken token) {
        if (token == null) {
            return false;
        }
        userTokens.remove(token.getId());
        if (token.getToken() != null) {
            userTokensByToken.remove(token.getToken());
        }
        return true;
    }

    // -------- Channel --------

    public void saveChannel(CPChannel channel) {
        if (channel.getId() == 0L) {
            channel.setId(nextId());
        }
        channels.put(channel.getId(), channel);
    }

    public CPChannel getChannelById(long id) {
        return channels.get(id);
    }

    public boolean deleteChannel(CPChannel channel) {
        if (channel == null) {
            return false;
        }
        channels.remove(channel.getId());
        return true;
    }

    public CPChannel[] getAllFixedChannel() {
        // For tests we simply return all channels whose owner == -1
        return channels.values().stream()
                .filter(c -> c.getOwner() == -1L)
                .toArray(CPChannel[]::new);
    }

    // -------- ChannelMember --------

    private String memberKey(long uid, long cid) {
        return uid + ":" + cid;
    }

    public void saveChannelMember(CPChannelMember member) {
        if (member.getId() == 0L) {
            member.setId(nextId());
        }
        channelMembers.put(member.getId(), member);
        channelMembersByUid
                .computeIfAbsent(member.getUid(), k -> new ArrayList<>())
                .removeIf(m -> m.getCid() == member.getCid());
        channelMembersByUid.get(member.getUid()).add(member);
        channelMembersByUidCid.put(memberKey(member.getUid(), member.getCid()), member);
    }

    public CPChannelMember getChannelMemberById(long id) {
        return channelMembers.get(id);
    }

    public CPChannelMember[] getAllChannelMember(long cid) {
        return channelMembers.values().stream()
                .filter(m -> m.getCid() == cid)
                .toArray(CPChannelMember[]::new);
    }

    public CPChannelMember getChannelMember(long uid, long cid) {
        return channelMembersByUidCid.get(memberKey(uid, cid));
    }

    public CPChannelMember[] getAllMemberByUserId(long uid) {
        List<CPChannelMember> list = channelMembersByUid.get(uid);
        if (list == null) {
            return new CPChannelMember[0];
        }
        return list.toArray(new CPChannelMember[0]);
    }

    public boolean deleteChannelMember(CPChannelMember member) {
        if (member == null) {
            return false;
        }
        channelMembers.remove(member.getId());
        List<CPChannelMember> list = channelMembersByUid.get(member.getUid());
        if (list != null) {
            list.removeIf(m -> m.getCid() == member.getCid());
        }
        channelMembersByUidCid.remove(memberKey(member.getUid(), member.getCid()));
        return true;
    }

    // -------- ChannelApplication --------

    public void saveChannelApplication(CPChannelApplication application) {
        if (application.getId() == 0L) {
            application.setId(nextId());
        }
        channelApplications.put(application.getId(), application);
    }

    public CPChannelApplication getChannelApplicationById(long id) {
        return channelApplications.get(id);
    }

    public CPChannelApplication getChannelApplicationByUidAndCid(long uid, long cid) {
        return channelApplications.values().stream()
                .filter(a -> Objects.equals(a.getUid(), uid) && Objects.equals(a.getCid(), cid))
                .findFirst()
                .orElse(null);
    }

    public CPChannelApplication[] getChannelApplicationsByCid(long cid) {
        return channelApplications.values().stream()
                .filter(a -> a.getCid() == cid)
                .toArray(CPChannelApplication[]::new);
    }

    // -------- ChannelBan --------

    private String banKey(long uid, long cid) {
        return uid + ":" + cid;
    }

    public void saveChannelBan(CPChannelBan ban) {
        if (ban.getId() == 0L) {
            ban.setId(nextId());
        }
        if (ban.getCreateTime() == null) {
            ban.setCreateTime(LocalDateTime.now());
        }
        channelBans.put(ban.getId(), ban);
        channelBansByUidCid.put(banKey(ban.getUid(), ban.getCid()), ban);
    }

    public CPChannelBan getChannelBanById(long id) {
        return channelBans.get(id);
    }

    public CPChannelBan getChannelBanByChannelIdAndUserId(long uid, long cid) {
        return channelBansByUidCid.get(banKey(uid, cid));
    }

    public CPChannelBan[] getChannelBansByCid(long cid) {
        return channelBans.values().stream()
                .filter(b -> b.getCid() == cid)
                .toArray(CPChannelBan[]::new);
    }

    public boolean deleteChannelBan(CPChannelBan ban) {
        if (ban == null) {
            return false;
        }
        channelBans.remove(ban.getId());
        channelBansByUidCid.remove(banKey(ban.getUid(), ban.getCid()));
        return true;
    }

    // -------- Message --------

    public void saveMessage(CPMessage message) {
        if (message.getId() == 0L) {
            message.setId(nextId());
        }
        messages.put(message.getId(), message);
    }

    public CPMessage getMessageById(long id) {
        return messages.get(id);
    }

    public CPMessage[] getMessagesBefore(long cid, LocalDateTime time, int count) {
        return messages.values().stream()
                .filter(m -> m.getCid() == cid && m.getSendTime() != null && !m.getSendTime().isAfter(time))
                .sorted(Comparator.comparing(CPMessage::getSendTime).reversed())
                .limit(count)
                .toArray(CPMessage[]::new);
    }

    public int getMessagesAfterCount(long cid, long uid, LocalDateTime time) {
        return (int) messages.values().stream()
                .filter(m -> m.getCid() == cid
                        && m.getUid() == uid
                        && m.getSendTime() != null
                        && m.getSendTime().isAfter(time))
                .count();
    }

    public boolean deleteMessage(CPMessage message) {
        if (message == null) {
            return false;
        }
        messages.remove(message.getId());
        return true;
    }

    // -------- Utilities --------

    public void clearAll() {
        users.clear();
        userTokens.clear();
        userTokensByToken.clear();
        channels.clear();
        channelMembers.clear();
        channelMembersByUid.clear();
        channelMembersByUidCid.clear();
        channelApplications.clear();
        channelBans.clear();
        channelBansByUidCid.clear();
        messages.clear();
    }
}
