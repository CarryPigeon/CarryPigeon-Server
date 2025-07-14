package team.carrypigeon.backend.dao.impl.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.api.domain.bo.user.CPUserBO;
import team.carrypigeon.backend.dao.mapper.user.UserMapper;
import team.carrypigeon.backend.dao.mapper.user.UserPO;

@Component
@Slf4j
public class CPUserImpl implements CPUserDAO {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;



    @Override
    public CPUserBO getById(long id) {
        return userMapper.selectById(id).toUserBO(objectMapper);
    }

    @Override
    public boolean removeById(long id) {
        userMapper.deleteById(id);
        return true;
    }

    @Override
    public boolean register(CPUserBO user, String password) {
        UserPO userPO = new UserPO();
        userPO.fillData(user,password,objectMapper);
        userMapper.insert(userPO);
        return true;
    }

    @Override
    public boolean update(CPUserBO user, String password) {
        UserPO userPO = new UserPO();
        userPO.fillData(user,password,objectMapper);
        if(password.isEmpty()){
            userPO.setPassword(userMapper.selectById(user.getId()).getPassword());
        }
        userMapper.updateById(userPO);
        return true;
    }

    @Override
    public CPUserBO login(String email, String password) {
        return null;
    }
}
