package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.common.id.IdUtil;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户 Token 相关 Node 的单元测试集合，依赖真实 DAO。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class UserTokenNodeTest {

    @Autowired
    private CPUUserTokenCreatorNode userTokenCreatorNode;

    @Autowired
    private CPUserTokenDeleter userTokenDeleterNode;

    @Autowired
    private CPUserTokenRefreshNode userTokenRefreshNode;

    @Autowired
    private CPUserTokenUidGetterNode userTokenUidGetterNode;

    @Autowired
    private CPUserTokenSaverNode userTokenSaverNode;

    @Autowired
    private CPUserTokenSelectorNode userTokenSelectorNode;

    @Autowired
    private CPUserTokenUpdaterNode userTokenUpdaterNode;

    @Autowired
    private UserTokenDao userTokenDao;

    @Autowired
    private UserDao userDao;

    @Test
    void testCPUUserTokenCreatorNode_createsAndSavesToken() throws Exception {
        DefaultContext context = new DefaultContext();
        long uid = IdUtil.generateId();
        CPUser user = new CPUser().setId(uid).setEmail("token_user_" + uid + "@example.com");
        userDao.save(user);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO, user);

        userTokenCreatorNode.process(new TestSession(), context);

        CPUserToken token = context.getData(CPNodeValueKeyExtraConstants.USER_TOKEN);
        assertNotNull(token);
        assertEquals(user.getId(), token.getUid());
        CPUserToken fromDb = userTokenDao.getById(token.getId());
        assertNotNull(fromDb);
    }

    @Test
    void testCPUserTokenDeleter_deletesToken() throws Exception {
        DefaultContext context = new DefaultContext();
        CPUserToken token = new CPUserToken().setId(IdUtil.generateId()).setUid(IdUtil.generateId());
        userTokenDao.save(token);
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN, token);

        userTokenDeleterNode.process(new TestSession(), context);
        CPResponse response = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        assertNull(response);
    }

    @Test
    void testCPUserTokenDeleter_deleteFailureWritesErrorResponse() {
        DefaultContext context = new DefaultContext();
        CPUserToken token = new CPUserToken().setId(IdUtil.generateId()).setUid(IdUtil.generateId());
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN, token);

        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> userTokenDeleterNode.process(new TestSession(), context));
        assertNotNull(ex);
        CPResponse response = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        assertNotNull(response);
    }

    @Test
    void testCPUserTokenRefreshNode_refreshesExpireTimeAndToken() throws Exception {
        DefaultContext context = new DefaultContext();
        CPUserToken token = new CPUserToken()
                .setId(IdUtil.generateId())
                .setUid(IdUtil.generateId())
                .setToken("old")
                .setExpiredTime(LocalDateTime.now().minusDays(1));
        userTokenDao.save(token);
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN, token);

        userTokenRefreshNode.process(new TestSession(), context);

        CPUserToken refreshed = context.getData(CPNodeValueKeyExtraConstants.USER_TOKEN);
        assertNotNull(refreshed.getToken());
        assertTrue(refreshed.getExpiredTime().isAfter(LocalDateTime.now()));
    }

    @Test
    void testCPUserTokenUidGetterNode_putsUserIdIntoContext() throws Exception {
        DefaultContext context = new DefaultContext();
        CPUserToken token = new CPUserToken().setUid(99L);
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN, token);

        userTokenUidGetterNode.process(new TestSession(), context);

        Long uid = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_ID);
        assertEquals(99L, uid);
    }

    @Test
    void testCPUserTokenSaverNode_savesToken() throws Exception {
        DefaultContext context = new DefaultContext();
        CPUserToken token = new CPUserToken().setId(IdUtil.generateId()).setUid(IdUtil.generateId());
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN, token);

        userTokenSaverNode.process(new TestSession(), context);

        CPUserToken fromDb = userTokenDao.getById(token.getId());
        assertNotNull(fromDb);
    }

    @Test
    void testCPUserTokenSelectorNode_argsErrorWhenKeyMissing() {
        DefaultContext context = new DefaultContext();

        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> userTokenSelectorNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPUserTokenUpdaterNode_updatesFieldsWhenPresent() throws Exception {
        DefaultContext context = new DefaultContext();
        CPUserToken token = new CPUserToken()
                .setId(IdUtil.generateId())
                .setToken("old")
                .setExpiredTime(LocalDateTime.now().minusDays(1));
        userTokenDao.save(token);
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN, token);

        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN_TOKEN, "newToken");
        long newExpireMillis = System.currentTimeMillis() + 1000L;
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN_EXPIRED_TIME, newExpireMillis);

        userTokenUpdaterNode.process(new TestSession(), context);

        CPUserToken updated = context.getData(CPNodeValueKeyExtraConstants.USER_TOKEN);
        assertEquals("newToken", updated.getToken());
        assertTrue(updated.getExpiredTime().isAfter(LocalDateTime.now()));
    }
}