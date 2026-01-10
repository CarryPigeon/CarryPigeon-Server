package team.carrypigeon.backend.dao.database.mapper;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import team.carrypigeon.backend.dao.database.mapper.channel.ChannelPO;
import team.carrypigeon.backend.dao.database.mapper.channel.application.ChannelApplicationPO;
import team.carrypigeon.backend.dao.database.mapper.channel.ban.ChannelBanPO;
import team.carrypigeon.backend.dao.database.mapper.channel.member.ChannelMemberPO;
import team.carrypigeon.backend.dao.database.mapper.channel.read.ChannelReadStatePO;
import team.carrypigeon.backend.dao.database.mapper.file.FileInfoPO;
import team.carrypigeon.backend.dao.database.mapper.message.MessagePO;
import team.carrypigeon.backend.dao.database.mapper.user.UserPO;
import team.carrypigeon.backend.dao.database.mapper.user.token.UserTokenPO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DaoPoMappingTests {

    @Test
    void userPo_toBoAndFromBo_roundTrips() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPUser user = new CPUser()
                .setId(1L)
                .setUsername("u")
                .setAvatar(2L)
                .setEmail("a@b.com")
                .setSex(CPUserSexEnum.MALE)
                .setBrief("b")
                .setBirthday(now)
                .setRegisterTime(now);
        UserPO po = UserPO.fromBo(user);
        assertEquals(user.getId(), po.getId());
        assertEquals(user.getSex().getValue(), po.getSex());

        CPUser mapped = po.toBo();
        assertEquals(user.getId(), mapped.getId());
        assertEquals(user.getSex(), mapped.getSex());
    }

    @Test
    void userTokenPo_toBoAndFrom_roundTrips() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPUserToken token = new CPUserToken(1L, 2L, "t", now);
        UserTokenPO po = UserTokenPO.from(token);
        assertEquals(1L, po.getId());
        assertEquals("t", po.getToken());

        CPUserToken mapped = po.toBo();
        assertEquals(token.getId(), mapped.getId());
        assertEquals(token.getUid(), mapped.getUid());
    }

    @Test
    void channelPo_toBoAndFromBo_roundTrips() {
        CPChannel channel = new CPChannel()
                .setId(1L)
                .setName("c")
                .setOwner(2L)
                .setAvatar(3L)
                .setBrief("b");
        ChannelPO po = ChannelPO.fromBo(channel);
        assertEquals(channel.getOwner(), po.getOwner());

        CPChannel mapped = po.toBo();
        assertEquals(channel.getId(), mapped.getId());
        assertEquals(channel.getName(), mapped.getName());
    }

    @Test
    void channelApplicationPo_toBoAndFromBo_roundTrips() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPChannelApplication app = new CPChannelApplication()
                .setId(1L)
                .setCid(2L)
                .setUid(3L)
                .setState(CPChannelApplicationStateEnum.APPROVED)
                .setMsg("m")
                .setApplyTime(now);
        ChannelApplicationPO po = ChannelApplicationPO.fromBo(app);
        assertEquals(app.getState().getValue(), po.getState());

        CPChannelApplication mapped = po.toBo();
        assertEquals(app.getId(), mapped.getId());
        assertEquals(app.getState(), mapped.getState());
    }

    @Test
    void channelBanPo_toBoAndFromBo_roundTrips() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPChannelBan ban = new CPChannelBan()
                .setId(1L)
                .setCid(2L)
                .setUid(3L)
                .setAid(4L)
                .setDuration(5)
                .setCreateTime(now);
        ChannelBanPO po = ChannelBanPO.fromBo(ban);
        assertEquals(ban.getDuration(), po.getDuration());

        CPChannelBan mapped = po.toBo();
        assertEquals(ban.getAid(), mapped.getAid());
    }

    @Test
    void channelMemberPo_toBoAndFrom_roundTrips() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setName("n")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(now);
        ChannelMemberPO po = ChannelMemberPO.from(member);
        assertEquals(member.getAuthority().getAuthority(), po.getAuthority());

        CPChannelMember mapped = po.toBo();
        assertEquals(member.getAuthority(), mapped.getAuthority());
    }

    @Test
    void channelReadStatePo_toBoAndFromBo_roundTrips() {
        CPChannelReadState state = new CPChannelReadState()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setLastReadTime(456L);
        ChannelReadStatePO po = ChannelReadStatePO.fromBo(state);
        assertEquals(state.getLastReadTime(), po.getLastReadTime());

        CPChannelReadState mapped = po.toBo();
        assertEquals(state.getLastReadTime(), mapped.getLastReadTime());
    }

    @Test
    void fileInfoPo_toBoAndFromBo_roundTrips() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPFileInfo info = new CPFileInfo()
                .setId(1L)
                .setSha256("sha")
                .setSize(2L)
                .setObjectName("obj")
                .setContentType("ct")
                .setCreateTime(now);
        FileInfoPO po = FileInfoPO.fromBo(info);
        assertEquals(info.getSha256(), po.getSha256());

        CPFileInfo mapped = po.toBo();
        assertEquals(info.getContentType(), mapped.getContentType());
    }

    @Test
    void messagePo_toBoAndFromBo_roundTrips() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        JsonNode data = JsonNodeFactory.instance.objectNode().put("k", "v");
        CPMessage msg = new CPMessage()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setDomain("d")
                .setData(data)
                .setSendTime(now);
        MessagePO po = MessagePO.fromBo(msg);
        assertEquals(msg.getDomain(), po.getDomain());

        CPMessage mapped = po.toBo();
        assertEquals(msg.getSendTime(), mapped.getSendTime());
        assertEquals(msg.getData(), mapped.getData());
    }
}
