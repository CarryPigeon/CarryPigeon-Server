package team.carrypigeon.backend.chat.domain.controller.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.*;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list.CPChannelListApplicationResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list.CPChannelListBanResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list.CPChannelListBanResultItem;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get.CPChannelGetProfileResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.list.CPChannelListResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.member.get.CPChannelGetMemberResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list.CPChannelListMemberResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.create.CPChannelCreateResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.get.CPMessageGetResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create.CPMessageCreateResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.get.CPMessageReadStateGetResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get.CPMessageGetUnreadResult;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list.CPMessageListResult;
import team.carrypigeon.backend.chat.domain.controller.netty.file.CPFileDownloadTokenApplyResult;
import team.carrypigeon.backend.chat.domain.controller.netty.file.CPFileUploadTokenApplyResult;
import team.carrypigeon.backend.chat.domain.controller.netty.user.login.email.CPUserEmailLoginResult;
import team.carrypigeon.backend.chat.domain.controller.netty.user.login.token.CPUserTokenLoginResult;
import team.carrypigeon.backend.chat.domain.controller.netty.user.register.CPUserRegisterResult;
import team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get.CPUserGetProfileResult;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NettyResultNodesTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void channelListResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_LIST, Set.of(
                new CPChannel().setId(1L).setName("c").setOwner(2L).setAvatar(3L).setBrief("b")
        ));

        new CPChannelListResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
    }

    @Test
    void channelListApplicationResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_LIST, Set.of(
                new CPChannelApplication()
                        .setId(1L)
                        .setUid(2L)
                        .setCid(3L)
                        .setState(CPChannelApplicationStateEnum.PENDING)
                        .setMsg("m")
                        .setApplyTime(LocalDateTime.of(2025, 12, 13, 18, 45, 28))
        ));

        new CPChannelListApplicationResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
    }

    @Test
    void messageListResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_LIST, new CPMessage[]{
                new CPMessage()
                        .setId(1L)
                        .setUid(2L)
                        .setCid(3L)
                        .setDomain("d")
                        .setData(mapper.createObjectNode().put("k", "v"))
                        .setSendTime(LocalDateTime.of(2025, 12, 13, 18, 45, 28))
        });

        new CPMessageListResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
    }

    @Test
    void channelListMemberResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_LIST, Set.of(
                new CPChannelMember()
                        .setId(1L)
                        .setUid(2L)
                        .setCid(3L)
                        .setName("n")
                        .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                        .setJoinTime(LocalDateTime.of(2025, 12, 13, 18, 45, 28))
        ));

        new CPChannelListMemberResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
    }

    @Test
    void userGetProfileResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeUserKeys.USER_INFO, new CPUser()
                .setId(1L)
                .setUsername("u")
                .setAvatar(2L)
                .setEmail("a@b.com")
                .setSex(CPUserSexEnum.UNKNOWN)
                .setBrief("b")
                .setBirthday(LocalDateTime.of(2025, 12, 13, 18, 45, 28))
        );

        new CPUserGetProfileResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertEquals(TimeUtil.LocalDateTimeToMillis(LocalDateTime.of(2025, 12, 13, 18, 45, 28)),
                response.getData().get("birthday").asLong());
    }

    @Test
    void fileTokenApplyResults_shouldWriteResponse() {
        CPFlowContext uploadCtx = new CPFlowContext();
        uploadCtx.setData(CPNodeFileKeys.FILE_TOKEN, "t1");
        new CPFileUploadTokenApplyResult().process(new TestSession(), uploadCtx, mapper);
        assertSuccess(uploadCtx);

        CPFlowContext downloadCtx = new CPFlowContext();
        downloadCtx.setData(CPNodeFileKeys.FILE_TOKEN, "t2");
        new CPFileDownloadTokenApplyResult().process(new TestSession(), downloadCtx, mapper);
        assertSuccess(downloadCtx);
    }

    @Test
    void messageGetResult_shouldWriteResponse() {
        LocalDateTime sendTime = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPMessage message = new CPMessage()
                .setId(1L)
                .setUid(2L)
                .setCid(3L)
                .setDomain("d")
                .setData(mapper.createObjectNode().put("k", "v"))
                .setSendTime(sendTime);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, message);

        new CPMessageGetResult().process(new TestSession(), context, mapper);

        assertSuccess(context);
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertEquals(1L, response.getData().get("mid").asLong());
        assertEquals(2L, response.getData().get("uid").asLong());
        assertEquals(3L, response.getData().get("cid").asLong());
        assertEquals("d", response.getData().get("domain").asText());
        assertEquals("v", response.getData().get("data").get("k").asText());
        assertEquals(TimeUtil.LocalDateTimeToMillis(sendTime), response.getData().get("sendTime").asLong());
    }

    @Test
    void messageGetResult_missingMessage_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPMessageGetResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void channelGetProfileResult_shouldWriteResponse() {
        LocalDateTime createTime = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPChannel channel = new CPChannel()
                .setName("c")
                .setOwner(2L)
                .setBrief("b")
                .setAvatar(3L)
                .setCreateTime(createTime);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO, channel);

        new CPChannelGetProfileResult().process(new TestSession(), context, mapper);

        assertSuccess(context);
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertEquals("c", response.getData().get("name").asText());
        assertEquals(2L, response.getData().get("owner").asLong());
        assertEquals("b", response.getData().get("brief").asText());
        assertEquals(3L, response.getData().get("avatar").asLong());
        assertEquals(TimeUtil.LocalDateTimeToMillis(createTime), response.getData().get("createTime").asLong());
    }

    @Test
    void channelGetProfileResult_missingChannel_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPChannelGetProfileResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void channelGetMemberResult_shouldWriteResponse() {
        LocalDateTime joinTime = LocalDateTime.of(2025, 12, 13, 18, 45, 28);
        CPChannelMember member = new CPChannelMember()
                .setUid(2L)
                .setName("n")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(joinTime);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);

        new CPChannelGetMemberResult().process(new TestSession(), context, mapper);

        assertSuccess(context);
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertEquals(2L, response.getData().get("uid").asLong());
        assertEquals("n", response.getData().get("name").asText());
        assertEquals(CPChannelMemberAuthorityEnum.ADMIN.getAuthority(),
                response.getData().get("authority").asInt());
        assertEquals(TimeUtil.LocalDateTimeToMillis(joinTime), response.getData().get("joinTime").asLong());
    }

    @Test
    void channelGetMemberResult_missingMember_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPChannelGetMemberResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void channelListBanResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_ITEMS, List.of(
                new CPChannelListBanResultItem().setUid(1L).setAid(2L).setBanTime(3L).setDuration(4),
                new CPChannelListBanResultItem().setUid(5L).setAid(6L).setBanTime(7L).setDuration(8)
        ));

        new CPChannelListBanResult().process(new TestSession(), context, mapper);
        assertSuccess(context);

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertEquals(2, response.getData().get("count").asInt());
        assertEquals(1L, response.getData().get("bans").get(0).get("uid").asLong());
        assertEquals(8, response.getData().get("bans").get(1).get("duration").asInt());
    }

    @Test
    void channelListBanResult_missingItems_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPChannelListBanResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void userRegisterResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO, new CPUserToken().setToken("t"));

        new CPUserRegisterResult().process(new TestSession(), context, mapper);
        assertSuccess(context);

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertEquals("t", response.getData().get("token").asText());
    }

    @Test
    void userRegisterResult_missingUserToken_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPUserRegisterResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void userLoginResults_shouldWriteResponse() {
        CPFlowContext tokenCtx = new CPFlowContext();
        tokenCtx.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO, new CPUserToken().setUid(1L).setToken("t1"));
        new CPUserTokenLoginResult().process(new TestSession(), tokenCtx, mapper);
        assertSuccess(tokenCtx);
        assertEquals("t1", tokenCtx.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("token").asText());
        assertEquals(1L, tokenCtx.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("uid").asLong());

        CPFlowContext emailCtx = new CPFlowContext();
        emailCtx.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO, new CPUserToken().setToken("t2"));
        new CPUserEmailLoginResult().process(new TestSession(), emailCtx, mapper);
        assertSuccess(emailCtx);
        assertEquals("t2", emailCtx.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("token").asText());
    }

    @Test
    void userLoginResults_missingUserToken_shouldWriteArgsError() {
        CPFlowContext tokenCtx = new CPFlowContext();
        new CPUserTokenLoginResult().process(new TestSession(), tokenCtx, mapper);
        assertArgsError(tokenCtx);

        CPFlowContext emailCtx = new CPFlowContext();
        new CPUserEmailLoginResult().process(new TestSession(), emailCtx, mapper);
        assertArgsError(emailCtx);
    }

    @Test
    void channelCreateResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setId(123L));
        new CPChannelCreateResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
        assertEquals(123L, context.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("cid").asLong());
    }

    @Test
    void channelCreateResult_missingChannel_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPChannelCreateResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void messageCreateResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage().setId(123L));
        new CPMessageCreateResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
        assertEquals(123L, context.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("mid").asLong());
    }

    @Test
    void messageCreateResult_missingMessage_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPMessageCreateResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void messageGetUnreadResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeMessageKeys.MESSAGE_UNREAD_COUNT, 5L);
        new CPMessageGetUnreadResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
        assertEquals(5L, context.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("count").asLong());
    }

    @Test
    void messageGetUnreadResult_missingCount_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPMessageGetUnreadResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    @Test
    void messageReadStateGetResult_shouldWriteResponse() {
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO,
                new CPChannelReadState().setCid(1L).setUid(2L).setLastReadTime(3L));
        new CPMessageReadStateGetResult().process(new TestSession(), context, mapper);
        assertSuccess(context);
        assertEquals(1L, context.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("cid").asLong());
        assertEquals(2L, context.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("uid").asLong());
        assertEquals(3L, context.<CPResponse>getData(CPNodeCommonKeys.RESPONSE).getData().get("lastReadTime").asLong());
    }

    @Test
    void messageReadStateGetResult_missingState_shouldWriteArgsError() {
        CPFlowContext context = new CPFlowContext();
        new CPMessageReadStateGetResult().process(new TestSession(), context, mapper);
        assertArgsError(context);
    }

    private static void assertSuccess(CPFlowContext context) {
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
    }

    private static void assertArgsError(CPFlowContext context) {
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("invalid response args", response.getData().get("msg").asText());
    }
}
