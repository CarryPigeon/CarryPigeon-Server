package team.carrypigeon.backend.dao.database.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.dao.database.impl.channel.ChannelDaoImpl;
import team.carrypigeon.backend.dao.database.impl.channel.application.ChannelApplicationDaoImpl;
import team.carrypigeon.backend.dao.database.impl.channel.ban.ChannelBanDaoImpl;
import team.carrypigeon.backend.dao.database.impl.channel.member.ChannelMemberDaoImpl;
import team.carrypigeon.backend.dao.database.impl.channel.read.ChannelReadStateDaoImpl;
import team.carrypigeon.backend.dao.database.impl.file.FileInfoDaoImpl;
import team.carrypigeon.backend.dao.database.impl.message.MessageDaoImpl;
import team.carrypigeon.backend.dao.database.impl.user.UserDaoImpl;
import team.carrypigeon.backend.dao.database.impl.user.token.UserTokenDaoImpl;
import team.carrypigeon.backend.dao.database.mapper.channel.ChannelMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.ChannelPO;
import team.carrypigeon.backend.dao.database.mapper.channel.application.ChannelApplicationMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.application.ChannelApplicationPO;
import team.carrypigeon.backend.dao.database.mapper.channel.ban.ChannelBanMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.ban.ChannelBanPO;
import team.carrypigeon.backend.dao.database.mapper.channel.member.ChannelMemberMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.member.ChannelMemberPO;
import team.carrypigeon.backend.dao.database.mapper.channel.read.ChannelReadStateMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.read.ChannelReadStatePO;
import team.carrypigeon.backend.dao.database.mapper.file.FileInfoMapper;
import team.carrypigeon.backend.dao.database.mapper.file.FileInfoPO;
import team.carrypigeon.backend.dao.database.mapper.message.MessageMapper;
import team.carrypigeon.backend.dao.database.mapper.message.MessagePO;
import team.carrypigeon.backend.dao.database.mapper.user.UserMapper;
import team.carrypigeon.backend.dao.database.mapper.user.UserPO;
import team.carrypigeon.backend.dao.database.mapper.user.token.UserTokenMapper;
import team.carrypigeon.backend.dao.database.mapper.user.token.UserTokenPO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DaoImplTests {

    @Test
    void userDaoImpl_getSaveBranches_shouldWork() {
        UserMapper mapper = mock(UserMapper.class);
        UserDaoImpl dao = new UserDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new UserPO()
                .setId(1L)
                .setUsername("u1")
                .setAvatar(0L)
                .setEmail("a@b.com")
                .setSex(0));
        assertNotNull(dao.getById(1L));

        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectOne(any())).thenReturn(new UserPO()
                .setId(3L)
                .setUsername("u3")
                .setAvatar(0L)
                .setEmail("x@y.com")
                .setSex(1));
        assertNotNull(dao.getByEmail("x@y.com"));

        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(dao.getByEmail("nope"));

        assertFalse(dao.save(null));

        CPUser user = new CPUser().setId(1L).setEmail("a@b.com").setSex(CPUserSexEnum.UNKNOWN);
        when(mapper.insertOrUpdate(any(UserPO.class))).thenReturn(true);
        assertTrue(dao.save(user));

        when(mapper.insertOrUpdate(any(UserPO.class))).thenReturn(false);
        assertFalse(dao.save(user));
    }

    @Test
    void userTokenDaoImpl_allBranches_shouldWork() {
        UserTokenMapper mapper = mock(UserTokenMapper.class);
        UserTokenDaoImpl dao = new UserTokenDaoImpl(mapper);

        when(mapper.selectOne(any())).thenReturn(new UserTokenPO().setId(1L).setUid(2L).setToken("t"));
        assertNotNull(dao.getByToken("t"));

        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(dao.getByToken("missing"));

        when(mapper.selectById(1L)).thenReturn(new UserTokenPO().setId(1L).setUid(2L).setToken("t"));
        assertNotNull(dao.getById(1L));

        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectList(any())).thenReturn(List.of(
                new UserTokenPO().setId(1L).setUid(2L).setToken("t1"),
                new UserTokenPO().setId(2L).setUid(2L).setToken("t2")
        ));
        assertEquals(2, dao.getByUserId(2L).length);

        assertFalse(dao.save(null));

        CPUserToken token = new CPUserToken(1L, 2L, "t", LocalDateTime.now());
        when(mapper.insertOrUpdate(any(UserTokenPO.class))).thenReturn(true);
        assertTrue(dao.save(token));
        when(mapper.insertOrUpdate(any(UserTokenPO.class))).thenReturn(false);
        assertFalse(dao.save(token));

        assertFalse(dao.delete(null));
        when(mapper.deleteById(1L)).thenReturn(1);
        assertTrue(dao.delete(token));
        when(mapper.deleteById(1L)).thenReturn(0);
        assertFalse(dao.delete(token));
    }

    @Test
    void channelDaoImpl_allBranches_shouldWork() {
        ChannelMapper mapper = mock(ChannelMapper.class);
        ChannelDaoImpl dao = new ChannelDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new ChannelPO()
                .setId(1L)
                .setOwner(-1L)
                .setAvatar(0L));
        assertNotNull(dao.getById(1L));

        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectList(any())).thenReturn(List.of(new ChannelPO()
                .setId(1L)
                .setOwner(-1L)
                .setAvatar(0L)));
        assertEquals(1, dao.getAllFixed().length);

        assertFalse(dao.save(null));
        CPChannel channel = new CPChannel().setId(1L).setOwner(2L);
        when(mapper.insertOrUpdate(any(ChannelPO.class))).thenReturn(true);
        assertTrue(dao.save(channel));
        when(mapper.insertOrUpdate(any(ChannelPO.class))).thenReturn(false);
        assertFalse(dao.save(channel));

        assertFalse(dao.delete(null));
        when(mapper.deleteById(1L)).thenReturn(1);
        assertTrue(dao.delete(channel));
        when(mapper.deleteById(1L)).thenReturn(0);
        assertFalse(dao.delete(channel));
    }

    @Test
    void channelBanDaoImpl_allBranches_shouldWork() {
        ChannelBanMapper mapper = mock(ChannelBanMapper.class);
        ChannelBanDaoImpl dao = new ChannelBanDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new ChannelBanPO()
                .setId(1L)
                .setCid(1L)
                .setUid(2L)
                .setAid(3L));
        assertNotNull(dao.getById(1L));
        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectList(any())).thenReturn(List.of(
                new ChannelBanPO().setId(1L).setCid(1L).setUid(2L).setAid(3L),
                new ChannelBanPO().setId(2L).setCid(1L).setUid(3L).setAid(3L)
        ));
        assertEquals(2, dao.getByChannelId(1L).length);

        when(mapper.selectOne(any())).thenReturn(new ChannelBanPO()
                .setId(3L)
                .setCid(2L)
                .setUid(1L)
                .setAid(3L));
        assertNotNull(dao.getByChannelIdAndUserId(1L, 2L));
        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(dao.getByChannelIdAndUserId(1L, 2L));

        assertFalse(dao.save(null));
        CPChannelBan ban = new CPChannelBan().setId(1L).setCid(2L).setUid(3L);
        when(mapper.insertOrUpdate(any(ChannelBanPO.class))).thenReturn(true);
        assertTrue(dao.save(ban));
        when(mapper.insertOrUpdate(any(ChannelBanPO.class))).thenReturn(false);
        assertFalse(dao.save(ban));

        assertFalse(dao.delete(null));
        when(mapper.deleteById(1L)).thenReturn(1);
        assertTrue(dao.delete(ban));
        when(mapper.deleteById(1L)).thenReturn(0);
        assertFalse(dao.delete(ban));
    }

    @Test
    void channelMemberDaoImpl_allBranches_shouldWork() {
        ChannelMemberMapper mapper = mock(ChannelMemberMapper.class);
        ChannelMemberDaoImpl dao = new ChannelMemberDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new ChannelMemberPO()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setAuthority(0));
        assertNotNull(dao.getById(1L));
        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectList(any())).thenReturn(List.of(new ChannelMemberPO()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setAuthority(0)));
        assertEquals(1, dao.getAllMember(1L).length);
        assertEquals(1, dao.getAllMemberByUserId(1L).length);

        when(mapper.selectOne(any())).thenReturn(new ChannelMemberPO()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setAuthority(1));
        assertNotNull(dao.getMember(1L, 2L));
        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(dao.getMember(1L, 2L));

        assertFalse(dao.save(null));
        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN);
        when(mapper.insertOrUpdate(any(ChannelMemberPO.class))).thenReturn(true);
        assertTrue(dao.save(member));
        when(mapper.insertOrUpdate(any(ChannelMemberPO.class))).thenReturn(false);
        assertFalse(dao.save(member));

        assertFalse(dao.delete(null));
        when(mapper.deleteById(1L)).thenReturn(1);
        assertTrue(dao.delete(member));
        when(mapper.deleteById(1L)).thenReturn(0);
        assertFalse(dao.delete(member));
    }

    @Test
    void channelReadStateDaoImpl_allBranches_shouldWork() {
        ChannelReadStateMapper mapper = mock(ChannelReadStateMapper.class);
        ChannelReadStateDaoImpl dao = new ChannelReadStateDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new ChannelReadStatePO().setId(1L));
        assertNotNull(dao.getById(1L));
        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectOne(any())).thenReturn(new ChannelReadStatePO().setId(3L));
        assertNotNull(dao.getByUidAndCid(1L, 2L));
        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(dao.getByUidAndCid(1L, 2L));

        assertFalse(dao.save(null));
        CPChannelReadState state = new CPChannelReadState().setId(1L).setUid(2L).setCid(3L);
        when(mapper.insertOrUpdate(any(ChannelReadStatePO.class))).thenReturn(true);
        assertTrue(dao.save(state));
        when(mapper.insertOrUpdate(any(ChannelReadStatePO.class))).thenReturn(false);
        assertFalse(dao.save(state));

        assertFalse(dao.delete(null));
        when(mapper.deleteById(1L)).thenReturn(1);
        assertTrue(dao.delete(state));
        when(mapper.deleteById(1L)).thenReturn(0);
        assertFalse(dao.delete(state));
    }

    @Test
    void channelApplicationDaoImpl_allBranches_shouldWork() {
        ChannelApplicationMapper mapper = mock(ChannelApplicationMapper.class);
        ChannelApplicationDaoImpl dao = new ChannelApplicationDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new ChannelApplicationPO().setId(1L).setState(0));
        assertNotNull(dao.getById(1L));
        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectOne(any())).thenReturn(new ChannelApplicationPO().setId(3L).setState(0));
        assertNotNull(dao.getByUidAndCid(1L, 2L));
        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(dao.getByUidAndCid(1L, 2L));

        Page<ChannelApplicationPO> page = new Page<>(1, 10);
        page.setRecords(List.of(new ChannelApplicationPO().setId(1L).setState(0)));
        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);
        assertEquals(1, dao.getByCid(1L, 1, 10).length);

        assertFalse(dao.save(null));
        CPChannelApplication app = new CPChannelApplication()
                .setId(1L)
                .setCid(2L)
                .setUid(3L)
                .setState(CPChannelApplicationStateEnum.PENDING);
        when(mapper.insertOrUpdate(any(ChannelApplicationPO.class))).thenReturn(true);
        assertTrue(dao.save(app));
        when(mapper.insertOrUpdate(any(ChannelApplicationPO.class))).thenReturn(false);
        assertFalse(dao.save(app));
    }

    @Test
    void messageDaoImpl_allBranches_shouldWork() {
        MessageMapper mapper = mock(MessageMapper.class);
        MessageDaoImpl dao = new MessageDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new MessagePO()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setData("{\"k\":\"v\"}"));
        assertNotNull(dao.getById(1L));
        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectList(any())).thenReturn(List.of(
                new MessagePO().setId(1L).setUid(2L).setCid(3L).setData("{\"k\":\"v\"}"),
                new MessagePO().setId(2L).setUid(2L).setCid(3L).setData("{\"k\":\"v2\"}")
        ));
        assertEquals(2, dao.getBefore(1L, LocalDateTime.now(), 10).length);

        when(mapper.selectCount(any())).thenReturn(5L);
        assertEquals(5, dao.getAfterCount(1L, 2L, LocalDateTime.now()));

        assertFalse(dao.save(null));
        CPMessage msg = new CPMessage().setId(1L).setCid(2L).setUid(3L);
        when(mapper.insertOrUpdate(any(MessagePO.class))).thenReturn(true);
        assertTrue(dao.save(msg));
        when(mapper.insertOrUpdate(any(MessagePO.class))).thenReturn(false);
        assertFalse(dao.save(msg));

        assertFalse(dao.delete(null));
        when(mapper.deleteById(1L)).thenReturn(1);
        assertTrue(dao.delete(msg));
        when(mapper.deleteById(1L)).thenReturn(0);
        assertFalse(dao.delete(msg));
    }

    @Test
    void fileInfoDaoImpl_allBranches_shouldWork() {
        FileInfoMapper mapper = mock(FileInfoMapper.class);
        FileInfoDaoImpl dao = new FileInfoDaoImpl(mapper);

        when(mapper.selectById(1L)).thenReturn(new FileInfoPO()
                .setId(1L)
                .setSize(1L));
        assertNotNull(dao.getById(1L));
        when(mapper.selectById(2L)).thenReturn(null);
        assertNull(dao.getById(2L));

        when(mapper.selectOne(any())).thenReturn(new FileInfoPO()
                .setId(3L)
                .setSize(1L));
        assertNotNull(dao.getBySha256AndSize("sha", 1L));
        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(dao.getBySha256AndSize("sha", 1L));

        assertFalse(dao.save(null));
        CPFileInfo info = new CPFileInfo().setId(1L).setSha256("sha").setSize(1L);
        when(mapper.insert(any(FileInfoPO.class))).thenReturn(1);
        assertTrue(dao.save(info));
        when(mapper.insert(any(FileInfoPO.class))).thenReturn(0);
        assertFalse(dao.save(info));
    }
}
