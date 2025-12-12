package team.carrypigeon.backend.chat.domain.controller;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.controller.netty.user.login.email.CPUserEmailLoginVO;
import team.carrypigeon.backend.chat.domain.controller.netty.user.login.token.CPUserTokenLoginVO;
import team.carrypigeon.backend.chat.domain.controller.netty.user.login.token.logout.CPUserTokenLogoutVO;
import team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get.CPUserGetProfileVO;
import team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update.CPUserUpdateProfileVO;
import team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update.email.CPUserUpdateEmailProfileVO;
import team.carrypigeon.backend.chat.domain.controller.netty.user.register.CPUserRegisterVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;

/**
 * LiteFlow 集成测试：覆盖用户相关 Controller 对应的链路。
 *
 * 测试流程：
 * 1. 通过 DAO / CPCache 预置数据；
 * 2. 构造 DefaultContext 与 TestSession，调用对应 LiteFlow chain；
 * 3. 校验链路执行结果与数据库状态；
 * 4. 必要时清理内存数据库状态（当前 InMemory 实现每个测试独立实例，不共享状态）。
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class UserControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private CPCache cache;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserTokenDao userTokenDao;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @After
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    // ---------- /core/user/register ----------

    @Test
    public void testUserRegister_success() {
        String email = "register_success@test.com";
        int code = 123456;
        cache.set(email + ":code", String.valueOf(code), 300);

        DefaultContext context = new DefaultContext();
        context.setData("session", new TestSession());

        CPUserRegisterVO vo = new CPUserRegisterVO(email, code);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/register", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPUser saved = userDao.getByEmail(email);
        Assert.assertNotNull(saved);
        System.out.println( saved);
    }

    @Test
    public void testUserRegister_emailInvalid() {
        String email = "bad-email";
        int code = 123456;
        cache.set(email + ":code", String.valueOf(code), 300);

        DefaultContext context = new DefaultContext();
        context.setData("session", new TestSession());

        CPUserRegisterVO vo = new CPUserRegisterVO(email, code);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/register", null, context);
        Assert.assertFalse(resp.isSuccess());
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        Assert.assertNotNull(response);
        Assert.assertEquals(100, response.getCode());
    }

    @Test
    public void testUserRegister_emailExists() {
        String email = "exists@test.com";
        int code = 123456;
        cache.set(email + ":code", String.valueOf(code), 300);

        // 预置已存在用户
        CPUser existing = new CPUser();
        existing.setId(1L).setEmail(email);
        userDao.save(existing);

        DefaultContext context = new DefaultContext();
        context.setData("session", new TestSession());

        CPUserRegisterVO vo = new CPUserRegisterVO(email, code);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/register", null, context);
        Assert.assertFalse(resp.isSuccess());

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        Assert.assertNotNull(response);
        Assert.assertEquals(100, response.getCode());
    }

    @Test
    public void testUserRegister_emailCodeError() {
        String email = "code_error@test.com";
        int correctCode = 111111;
        int wrongCode = 222222;
        cache.set(email + ":code", String.valueOf(correctCode), 300);

        DefaultContext context = new DefaultContext();
        context.setData("session", new TestSession());

        CPUserRegisterVO vo = new CPUserRegisterVO(email, wrongCode);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/register", null, context);
        Assert.assertFalse(resp.isSuccess());

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        Assert.assertNotNull(response);
        Assert.assertEquals(100, response.getCode());
    }

    // ---------- /core/user/profile/get ----------

    @Test
    public void testUserProfileGet_success() {
        CPUser user = new CPUser();
        user.setId(100L).setEmail("profile_get@test.com");
        userDao.save(user);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, user.getId());

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPUserGetProfileVO vo = new CPUserGetProfileVO(user.getId());
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/profile/get", null, context);
        Assert.assertTrue(resp.isSuccess());
    }

    @Test
    public void testUserProfileGet_notLogin() {
        CPUser user = new CPUser();
        user.setId(101L).setEmail("profile_get_not_login@test.com");
        userDao.save(user);

        // 不设置 Session 中的 userId，触发 UserLoginChecker 失败
        TestSession session = new TestSession();

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPUserGetProfileVO vo = new CPUserGetProfileVO(user.getId());
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/profile/get", null, context);
        Assert.assertFalse(resp.isSuccess());

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        Assert.assertNotNull(response);
        Assert.assertEquals(300, response.getCode());
    }

    // ---------- /core/user/profile/update ----------

    @Test
    public void testUserProfileUpdate_success() {
        CPUser user = new CPUser();
        user.setId(200L).setEmail("profile_update@test.com");
        userDao.save(user);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, user.getId());

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPUserUpdateProfileVO vo = new CPUserUpdateProfileVO(
                "newName", 1L, 0, "brief", System.currentTimeMillis()
        );
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/profile/update", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPUser updated = userDao.getById(user.getId());
        Assert.assertNotNull(updated);
        Assert.assertEquals("newName", updated.getUsername());
    }

    // ---------- /core/user/profile/update/email ----------

    @Test
    public void testUserProfileUpdateEmail_success() {
        CPUser user = new CPUser();
        user.setId(300L).setEmail("old@test.com");
        userDao.save(user);

        String newEmail = "new_email@test.com";
        int code = 654321;
        cache.set(newEmail + ":code", String.valueOf(code), 300);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, user.getId());

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPUserUpdateEmailProfileVO vo = new CPUserUpdateEmailProfileVO(newEmail, code);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/profile/update/email", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPUser updated = userDao.getById(user.getId());
        Assert.assertNotNull(updated);
        Assert.assertEquals(newEmail, updated.getEmail());
    }

    // ---------- /core/user/login/email ----------

    @Test
    public void testUserLoginEmail_success() {
        String email = "login_email@test.com";
        int code = 888888;
        cache.set(email + ":code", String.valueOf(code), 300);

        CPUser user = new CPUser();
        user.setId(400L).setEmail(email);
        userDao.save(user);

        DefaultContext context = new DefaultContext();
        context.setData("session", new TestSession());

        CPUserEmailLoginVO vo = new CPUserEmailLoginVO(email, code);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/login/email", null, context);
        Assert.assertTrue(resp.isSuccess());
    }

    // ---------- /core/user/login/token ----------

    @Test
    public void testUserLoginToken_success() {
        CPUser user = new CPUser();
        user.setId(500L).setEmail("login_token@test.com");
        userDao.save(user);

        CPUserToken token = new CPUserToken();
        token.setId(1L).setUid(user.getId()).setToken("TOKEN_500");
        userTokenDao.save(token);

        TestSession session = new TestSession();

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPUserTokenLoginVO vo = new CPUserTokenLoginVO("TOKEN_500");
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/login/token", null, context);
        Assert.assertTrue(resp.isSuccess());

        Long loggedUid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        Assert.assertEquals(user.getId(), loggedUid.longValue());
    }

    // ---------- /core/user/login/token/logout ----------

    @Test
    public void testUserLogoutToken_success() {
        CPUser user = new CPUser();
        user.setId(600L).setEmail("logout_token@test.com");
        userDao.save(user);

        CPUserToken token = new CPUserToken();
        token.setId(2L).setUid(user.getId()).setToken("TOKEN_600");
        userTokenDao.save(token);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, user.getId());

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPUserTokenLogoutVO vo = new CPUserTokenLogoutVO("TOKEN_600");
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/user/login/token/logout", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPUserToken deleted = userTokenDao.getByToken("TOKEN_600");
        Assert.assertNull(deleted);
    }
}
