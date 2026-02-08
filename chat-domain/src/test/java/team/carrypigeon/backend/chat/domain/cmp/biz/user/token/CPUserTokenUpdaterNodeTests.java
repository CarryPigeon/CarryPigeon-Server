package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPUserTokenUpdaterNodeTests {

    @Test
    void process_shouldUpdateFieldsAndSave() throws Exception {
        UserTokenDao dao = mock(UserTokenDao.class);
        when(dao.save(any())).thenReturn(true);

        CPUserTokenUpdaterNode node = new CPUserTokenUpdaterNode(dao);

        CPUserToken token = new CPUserToken()
                .setId(1L)
                .setToken("old");

        long expiredMillis = 1700000000000L;
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO, token);
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, "new");
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO_EXPIRED_TIME, expiredMillis);

        node.process(null, context);

        assertEquals("new", token.getToken());
        assertEquals(TimeUtil.millisToLocalDateTime(expiredMillis), token.getExpiredTime());
        verify(dao).save(token);
    }

    @Test
    void process_saveFail_shouldThrowAndSetBusinessError() {
        UserTokenDao dao = mock(UserTokenDao.class);
        when(dao.save(any())).thenReturn(false);

        CPUserTokenUpdaterNode node = new CPUserTokenUpdaterNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO, new CPUserToken().setId(1L));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(500, ex.getProblem().status());
        assertEquals("internal_error", ex.getProblem().reason());
    }
}
