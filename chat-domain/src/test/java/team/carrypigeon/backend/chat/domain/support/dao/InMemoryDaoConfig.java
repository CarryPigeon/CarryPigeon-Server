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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryUserDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPUser getById(long id) {
            return db.getUserById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param email 用户邮箱
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPUser getByEmail(String email) {
            return db.getUserByEmail(email);
        }

        /**
         * 保存测试数据。
         *
         * @param user 用户实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPUser user) {
            db.saveUser(user);
            return true;
        }

        /**
         * 批量查询测试数据。
         *
         * @param ids 主键集合
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public List<CPUser> listByIds(Collection<Long> ids) {
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            return ids.stream()
                    .map(db::getUserById)
                    .filter(u -> u != null)
                    .toList();
        }
    }

    @Component
    public static class InMemoryUserTokenDao implements UserTokenDao {
        private final InMemoryDatabase db;

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryUserTokenDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPUserToken getById(long id) {
            return db.getUserTokenById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param userId 用户 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPUserToken[] getByUserId(long userId) {
            return db.getUserTokensByUserId(userId);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param token 令牌字符串
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPUserToken getByToken(String token) {
            return db.getUserTokenByToken(token);
        }

        /**
         * 保存测试数据。
         *
         * @param token 令牌字符串
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPUserToken token) {
            db.saveUserToken(token);
            return true;
        }

        /**
         * 删除测试数据。
         *
         * @param token 令牌字符串
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean delete(CPUserToken token) {
            return db.deleteUserToken(token);
        }
    }

    @Component
    public static class InMemoryChannelDao implements ChannelDao {
        private final InMemoryDatabase db;

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryChannelDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannel getById(long id) {
            return db.getChannelById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannel[] getAllFixed() {
            return db.getAllFixedChannel();
        }

        /**
         * 保存测试数据。
         *
         * @param channel 频道实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPChannel channel) {
            db.saveChannel(channel);
            return true;
        }

        /**
         * 删除测试数据。
         *
         * @param cpChannel 频道实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean delete(CPChannel cpChannel) {
            return db.deleteChannel(cpChannel);
        }
    }

    @Component
    public static class InMemoryChannelMemberDao implements ChannelMemberDao {
        private final InMemoryDatabase db;

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryChannelMemberDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelMember getById(long id) {
            return db.getChannelMemberById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param cid 频道 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelMember[] getAllMember(long cid) {
            return db.getAllChannelMember(cid);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param uid 用户 ID
         * @param cid 频道 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelMember getMember(long uid, long cid) {
            return db.getChannelMember(uid, cid);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param uid 用户 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelMember[] getAllMemberByUserId(long uid) {
            return db.getAllMemberByUserId(uid);
        }

        /**
         * 保存测试数据。
         *
         * @param channelMember 频道成员实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPChannelMember channelMember) {
            db.saveChannelMember(channelMember);
            return true;
        }

        /**
         * 删除测试数据。
         *
         * @param cpChannelMember 频道成员实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean delete(CPChannelMember cpChannelMember) {
            return db.deleteChannelMember(cpChannelMember);
        }
    }

    @Component
    public static class InMemoryChannelApplicationDao implements ChannelApplicationDAO {
        private final InMemoryDatabase db;

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryChannelApplicationDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 保存测试数据。
         *
         * @param application 入群申请实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPChannelApplication application) {
            db.saveChannelApplication(application);
            return true;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelApplication getById(long id) {
            return db.getChannelApplicationById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param uid 用户 ID
         * @param cid 频道 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelApplication getByUidAndCid(long uid, long cid) {
            return db.getChannelApplicationByUidAndCid(uid, cid);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param cid 频道 ID
         * @param page 页码（从 1 开始）
         * @param pageSize 每页条数
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelApplication[] getByCid(long cid, int page, int pageSize) {
            return db.getChannelApplicationsByCid(cid);
        }
    }

    @Component
    public static class InMemoryChannelBanDao implements ChannelBanDAO {
        private final InMemoryDatabase db;

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryChannelBanDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelBan getById(long id) {
            return db.getChannelBanById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param cid 频道 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelBan[] getByChannelId(long cid) {
            return db.getChannelBansByCid(cid);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param uid 用户 ID
         * @param cid 频道 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelBan getByChannelIdAndUserId(long uid, long cid) {
            return db.getChannelBanByChannelIdAndUserId(uid, cid);
        }

        /**
         * 保存测试数据。
         *
         * @param ban 封禁实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPChannelBan ban) {
            db.saveChannelBan(ban);
            return true;
        }

        /**
         * 删除测试数据。
         *
         * @param ban 封禁实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean delete(CPChannelBan ban) {
            return db.deleteChannelBan(ban);
        }
    }

    @Component
    public static class InMemoryChannelMessageDao implements ChannelMessageDao {
        private final InMemoryDatabase db;

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryChannelMessageDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPMessage getById(long id) {
            return db.getMessageById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param cid 频道 ID
         * @param cursorMid 分页游标消息 ID
         * @param count 单页条数
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPMessage[] listBefore(long cid, long cursorMid, int count) {
            return db.getMessagesBeforeMid(cid, cursorMid, count);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param cid 频道 ID
         * @param startMid 统计起始消息 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public int countAfter(long cid, long startMid) {
            return db.getMessagesAfterMidCount(cid, startMid);
        }

        /**
         * 保存测试数据。
         *
         * @param message 消息实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPMessage message) {
            db.saveMessage(message);
            return true;
        }

        /**
         * 删除测试数据。
         *
         * @param message 消息实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean delete(CPMessage message) {
            return db.deleteMessage(message);
        }
    }

    @Component
    public static class InMemoryChannelReadStateDao implements ChannelReadStateDao {
        private final InMemoryDatabase db;

        /**
         * 构造测试辅助对象。
         *
         * @param db 共享内存数据库实例
         */
        public InMemoryChannelReadStateDao(InMemoryDatabase db) {
            this.db = db;
        }

        /**
         * 按主键查询测试数据。
         *
         * @param id 主键 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelReadState getById(long id) {
            return db.getChannelReadStateById(id);
        }

        /**
         * 测试用内存 DAO 操作。
         *
         * @param uid 用户 ID
         * @param cid 频道 ID
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public CPChannelReadState getByUidAndCid(long uid, long cid) {
            return db.getChannelReadStateByUidAndCid(uid, cid);
        }

        /**
         * 保存测试数据。
         *
         * @param state 已读状态实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean save(CPChannelReadState state) {
            db.saveChannelReadState(state);
            return true;
        }

        /**
         * 删除测试数据。
         *
         * @param state 已读状态实体
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean delete(CPChannelReadState state) {
            return db.deleteChannelReadState(state);
        }
    }

    @Component
    public static class InMemoryCPCache implements CPCache {

        private final Map<String, ValueWithExpire> store = new HashMap<>();

        /**
         * 写入测试数据。
         *
         * @param key 缓存键
         * @param value 缓存值
         * @param expireTime 过期秒数
         */
        @Override
        public void set(String key, String value, int expireTime) {
            long expireAt = System.currentTimeMillis() + expireTime * 1000L;
            store.put(key, new ValueWithExpire(value, expireAt));
        }

        /**
         * 返回测试数据。
         *
         * @param key 缓存键
         * @return 与当前内存 DAO 操作对应的返回结果
         */
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

        /**
         * 读取并删除测试数据。
         *
         * @param key 缓存键
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public String getAndDelete(String key) {
            ValueWithExpire v = store.remove(key);
            if (v == null || v.isExpired()) {
                return null;
            }
            return v.value;
        }

        /**
         * 读取旧值并写入新值。
         *
         * @param key 缓存键
         * @param value 缓存值
         * @param expireTime 过期秒数
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public String getAndSet(String key, String value, int expireTime) {
            String old = get(key);
            set(key, value, expireTime);
            return old;
        }

        /**
         * 判断测试数据是否存在。
         *
         * @param key 缓存键
         * @return 与当前内存 DAO 操作对应的返回结果
         */
        @Override
        public boolean exists(String key) {
            return get(key) != null;
        }

        /**
         * 删除测试数据。
         *
         * @param key 缓存键
         * @return 与当前内存 DAO 操作对应的返回结果
         */
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
