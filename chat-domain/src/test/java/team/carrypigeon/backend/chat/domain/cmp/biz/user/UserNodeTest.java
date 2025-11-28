package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户相关 Node 的单元测试集合。
 * 使用真实 DAO + MySQL，验证基础入参 / 出参行为。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class UserNodeTest {

    @Autowired
    private CPUserBuilderNode userBuilderNode;

    @Autowired
    private CPUserSaverNode userSaverNode;

    @Autowired
    private CPUserSelectorNode userSelectorNode;

    @Autowired
    private CPUserUpdaterNode userUpdaterNode;

    @Autowired
    private UserDao userDao;

    @Test
    void testCPUserBuilderNode_buildsUserFromContext() throws Exception {
        // 准备上下文数据
        DefaultContext context = new DefaultContext();
        long id = 1L;
        long avatar = 2L;
        long birthdayMillis = TimeUtil.getCurrentTime();
        long registerMillis = TimeUtil.getCurrentTime();

        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_ID, id);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_USER_NAME, "testUser");
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BRIEF, "brief");
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_AVATAR, avatar);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_EMAIL, "test@example.com");
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_SEX, CPUserSexEnum.MALE.getValue());
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BIRTHDAY, birthdayMillis);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_REGISTER_TIME, registerMillis);

        userBuilderNode.process(new TestSession(), context);

        CPUser user = context.getData(CPNodeValueKeyBasicConstants.USER_INFO);
        assertNotNull(user);
        assertEquals(id, user.getId());
        assertEquals("testUser", user.getUsername());
        assertEquals("brief", user.getBrief());
        assertEquals(avatar, user.getAvatar());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(CPUserSexEnum.MALE, user.getSex());
        assertEquals(birthdayMillis, TimeUtil.LocalDateTimeToMillis(user.getBirthday()));
        assertEquals(registerMillis, TimeUtil.LocalDateTimeToMillis(user.getRegisterTime()));
    }

    @Test
    void testCPUserSaverNode_savesUserSuccessfully() throws Exception {
        DefaultContext context = new DefaultContext();
        // 使用 builder 构造一个用户再保存，确保和业务路径一致
        long id = IdUtil.generateId();
        long avatar = 2L;
        long birthdayMillis = TimeUtil.getCurrentTime();
        long registerMillis = TimeUtil.getCurrentTime();
        String email = "user_" + id + "@example.com";

        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_ID, id);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_USER_NAME, "user_" + id);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BRIEF, "brief");
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_AVATAR, avatar);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_EMAIL, email);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_SEX, CPUserSexEnum.UNKNOWN.getValue());
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BIRTHDAY, birthdayMillis);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_REGISTER_TIME, registerMillis);

        userBuilderNode.process(new TestSession(), context);
        CPUser user = context.getData(CPNodeValueKeyBasicConstants.USER_INFO);

        userSaverNode.process(new TestSession(), context);

        CPUser fromDb = userDao.getByEmail(email);
        assertNotNull(fromDb);
        assertEquals(user.getId(), fromDb.getId());
        CPResponse response = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        assertNull(response);
    }

    @Test
    void testCPUserUpdaterNode_updatesMutableFields() throws Exception {
        DefaultContext context = new DefaultContext();
        CPUser user = new CPUser()
                .setId(1L)
                .setUsername("old")
                .setBrief("old")
                .setAvatar(1L)
                .setEmail("old@example.com")
                .setSex(CPUserSexEnum.UNKNOWN)
                .setBirthday(LocalDateTime.now());
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO, user);

        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_USER_NAME, "newUser");
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BRIEF, "newBrief");
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_AVATAR, 99L);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_EMAIL, "new@example.com");
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_SEX, CPUserSexEnum.FEMALE.getValue());
        long newBirthday = TimeUtil.getCurrentTime();
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BIRTHDAY, newBirthday);

        userUpdaterNode.process(new TestSession(), context);

        CPUser updated = context.getData(CPNodeValueKeyBasicConstants.USER_INFO);
        assertEquals("newUser", updated.getUsername());
        assertEquals("newBrief", updated.getBrief());
        assertEquals(99L, updated.getAvatar());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals(CPUserSexEnum.FEMALE, updated.getSex());
        assertEquals(newBirthday, TimeUtil.LocalDateTimeToMillis(updated.getBirthday()));
    }

    @Test
    void testCPUserSelectorNode_argsErrorWhenBindKeyMissing() {
        DefaultContext context = new DefaultContext();

        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> userSelectorNode.process(new TestSession(), context));
        assertNotNull(ex);

        CPResponse response = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        // argsError 默认会写入通用 ERROR_RESPONSE
        assertNotNull(response);
        assertEquals(CPResponse.ERROR_RESPONSE.getCode(), response.getCode());
    }
}
