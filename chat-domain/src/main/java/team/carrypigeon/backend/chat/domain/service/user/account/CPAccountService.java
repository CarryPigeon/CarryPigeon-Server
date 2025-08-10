package team.carrypigeon.backend.chat.domain.service.user.account;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.chat.domain.manager.email.CPEmailCodeManager;
import team.carrypigeon.backend.common.id.IdUtil;

@Component
@Slf4j
public class CPAccountService {

    private final CPUserDAO cpUserDAO;
    private final CPEmailCodeManager emailCodeManager;

    public CPAccountService(CPUserDAO cpUserDAO, CPEmailCodeManager emailCodeManager) {
        this.cpUserDAO = cpUserDAO;
        this.emailCodeManager = emailCodeManager;
    }

    /**
     * 注册用户
     */
    public CPResponse register(String email, int code,String name, String password) {
        // 邮箱认证
        if (!emailCodeManager.verify(email, code)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("email code error");
        }
        CPUserBO cpUserBO = new CPUserBO();
        cpUserBO.setId(IdUtil.generateId());
        cpUserBO.setName(name);
        cpUserBO.setEmail(email);
        cpUserBO.setIntroduction("");
        cpUserBO.setAuthority(1);
        cpUserBO.setStateId(IdUtil.generateId());
        cpUserBO.setProfile(-1);
        cpUserBO.setRegisterTime(System.currentTimeMillis());
        // 使用BCrypt加密密码
        boolean register = cpUserDAO.register(cpUserBO, BCrypt.hashpw(password, BCrypt.gensalt()));
        if (register) {
            return CPResponse.SUCCESS_RESPONSE.copy();
        }
        return CPResponse.ERROR_RESPONSE.copy();
    }

    /**
     * 更新用户数据
     * */
    public CPResponse update(long userId,String name,String introduction,long profile) {
        CPUserBO user = cpUserDAO.getById(userId);
        user.setProfile(profile);
        user.setName(name);
        user.setIntroduction(introduction);
        user.setStateId(IdUtil.generateId());
        boolean update = cpUserDAO.update(user, null);
        // TODO 通知所有人或者通知相关人
        if (update) {
            return CPResponse.SUCCESS_RESPONSE.copy();
        }
        return CPResponse.ERROR_RESPONSE.copy();
    }

    /**
     * 登录并发送登录密钥
     * */
    public CPResponse login(String email,String pwd,int code,String deviceName){
        if (!emailCodeManager.verify(email, code)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("email code error");
        }
        String key = cpUserDAO.generateToken(email, pwd,deviceName );
        if (key != null&&!key.isEmpty()){
            //TODO 发送登录密钥
            return CPResponse.SUCCESS_RESPONSE.copy();
        }
        return CPResponse.ERROR_RESPONSE.copy();
    }
}
