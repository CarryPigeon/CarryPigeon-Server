package team.carrypigeon.backend.chat.domain.support.dao;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementations of DAO and cache interfaces used in tests.
 * These beans are scanned instead of real DAO implementations to avoid
 * external database and Redis dependencies during tests.
 */
public class InMemoryDaoConfig {

    @Component
    public static class InMemoryUserDao implements UserDao {
        private final InMemoryDatabase db;

        public InMemoryUserDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public CPUser getById(long id) {
            return db.getUserById(id);
        }

        @Override
        public CPUser getByEmail(String email) {
            return db.getUserByEmail(email);
        }

        @Override
        public boolean save(CPUser user) {
            db.saveUser(user);
            return true;
        }
    }

    @Component
    public static class InMemoryUserTokenDao implements UserTokenDao {
        private final InMemoryDatabase db;

        public InMemoryUserTokenDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public CPUserToken getById(long id) {
            return db.getUserTokenById(id);
        }

        @Override
        public CPUserToken[] getByUserId(long userId) {
            return db.getUserTokensByUserId(userId);
        }

        @Override
        public CPUserToken getByToken(String token) {
            return db.getUserTokenByToken(token);
        }

        @Override
        public boolean save(CPUserToken token) {
            db.saveUserToken(token);
            return true;
        }

        @Override
        public boolean delete(CPUserToken token) {
            return db.deleteUserToken(token);
        }
    }

    @Component
    public static class InMemoryChannelDao implements ChannelDao {
        private final InMemoryDatabase db;

        public InMemoryChannelDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public CPChannel getById(long id) {
            return db.getChannelById(id);
        }

        @Override
        public CPChannel[] getAllFixed() {
            return db.getAllFixedChannel();
        }

        @Override
        public boolean save(CPChannel channel) {
            db.saveChannel(channel);
            return true;
        }

        @Override
        public boolean delete(CPChannel cpChannel) {
            return db.deleteChannel(cpChannel);
        }
    }

    @Component
    public static class InMemoryChannelMemberDao implements ChannelMemberDao {
        private final InMemoryDatabase db;

        public InMemoryChannelMemberDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public CPChannelMember getById(long id) {
            return db.getChannelMemberById(id);
        }

        @Override
        public CPChannelMember[] getAllMember(long cid) {
            return db.getAllChannelMember(cid);
        }

        @Override
        public CPChannelMember getMember(long uid, long cid) {
            return db.getChannelMember(uid, cid);
        }

        @Override
        public CPChannelMember[] getAllMemberByUserId(long uid) {
            return db.getAllMemberByUserId(uid);
        }

        @Override
        public boolean save(CPChannelMember channelMember) {
            db.saveChannelMember(channelMember);
            return true;
        }

        @Override
        public boolean delete(CPChannelMember cpChannelMember) {
            return db.deleteChannelMember(cpChannelMember);
        }
    }

    @Component
    public static class InMemoryChannelApplicationDao implements ChannelApplicationDAO {
        private final InMemoryDatabase db;

        public InMemoryChannelApplicationDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public boolean save(CPChannelApplication application) {
            db.saveChannelApplication(application);
            return true;
        }

        @Override
        public CPChannelApplication getById(long id) {
            return db.getChannelApplicationById(id);
        }

        @Override
        public CPChannelApplication getByUidAndCid(long uid, long cid) {
            return db.getChannelApplicationByUidAndCid(uid, cid);
        }

        @Override
        public CPChannelApplication[] getByCid(long cid, int page, int pageSize) {
            // For tests, ignore pagination and return all applications for the channel.
            return db.getChannelApplicationsByCid(cid);
        }
    }

    @Component
    public static class InMemoryChannelBanDao implements ChannelBanDAO {
        private final InMemoryDatabase db;

        public InMemoryChannelBanDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public CPChannelBan getById(long id) {
            return db.getChannelBanById(id);
        }

        @Override
        public CPChannelBan[] getByChannelId(long cid) {
            return db.getChannelBansByCid(cid);
        }

        @Override
        public CPChannelBan getByChannelIdAndUserId(long uid, long cid) {
            return db.getChannelBanByChannelIdAndUserId(uid, cid);
        }

        @Override
        public boolean save(CPChannelBan ban) {
            db.saveChannelBan(ban);
            return true;
        }

        @Override
        public boolean delete(CPChannelBan ban) {
            return db.deleteChannelBan(ban);
        }
    }

    @Component
    public static class InMemoryChannelMessageDao implements ChannelMessageDao {
        private final InMemoryDatabase db;

        public InMemoryChannelMessageDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public CPMessage getById(long id) {
            return db.getMessageById(id);
        }

        @Override
        public CPMessage[] getBefore(long cid, LocalDateTime time, int count) {
            return db.getMessagesBefore(cid, time, count);
        }

        @Override
        public int getAfterCount(long cid, long uid, LocalDateTime time) {
            return db.getMessagesAfterCount(cid, uid, time);
        }

        @Override
        public boolean save(CPMessage message) {
            db.saveMessage(message);
            return true;
        }

        @Override
        public boolean delete(CPMessage message) {
            return db.deleteMessage(message);
        }
    }

    @Component
    public static class InMemoryChannelReadStateDao implements ChannelReadStateDao {
        private final InMemoryDatabase db;

        public InMemoryChannelReadStateDao(InMemoryDatabase db) {
            this.db = db;
        }

        @Override
        public CPChannelReadState getById(long id) {
            return db.getChannelReadStateById(id);
        }

        @Override
        public CPChannelReadState getByUidAndCid(long uid, long cid) {
            return db.getChannelReadStateByUidAndCid(uid, cid);
        }

        @Override
        public boolean save(CPChannelReadState state) {
            db.saveChannelReadState(state);
            return true;
        }

        @Override
        public boolean delete(CPChannelReadState state) {
            return db.deleteChannelReadState(state);
        }
    }

    @Component
    public static class InMemoryCPCache implements CPCache {

        private final Map<String, ValueWithExpire> store = new HashMap<>();

        @Override
        public void set(String key, String value, int expireTime) {
            long expireAt = System.currentTimeMillis() + expireTime * 1000L;
            store.put(key, new ValueWithExpire(value, expireAt));
        }

        @Override
        public String get(String key) {
            ValueWithExpire v = store.get(key);
            if (v == null) {
                return null;
            }
            if (v.isExpired()) {
                store.remove(key);
                return null;
            }
            return v.value;
        }

        @Override
        public String getAndDelete(String key) {
            ValueWithExpire v = store.remove(key);
            if (v == null || v.isExpired()) {
                return null;
            }
            return v.value;
        }

        @Override
        public String getAndSet(String key, String value, int expireTime) {
            String old = get(key);
            set(key, value, expireTime);
            return old;
        }

        @Override
        public boolean exists(String key) {
            return get(key) != null;
        }

        @Override
        public boolean delete(String key) {
            return store.remove(key) != null;
        }

        private static class ValueWithExpire {
            final String value;
            final long expireAtMillis;

            ValueWithExpire(String value, long expireAtMillis) {
                this.value = value;
                this.expireAtMillis = expireAtMillis;
            }

            boolean isExpired() {
                return System.currentTimeMillis() > expireAtMillis;
            }
        }
    }
}
