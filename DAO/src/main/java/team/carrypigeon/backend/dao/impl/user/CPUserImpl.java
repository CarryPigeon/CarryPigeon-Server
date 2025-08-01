package team.carrypigeon.backend.dao.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;
import team.carrypigeon.backend.dao.mapper.user.UserMapper;
import team.carrypigeon.backend.dao.mapper.user.UserPO;

@Component
@Slf4j
public class CPUserImpl implements CPUserDAO {

    @Autowired
    private final UserMapper userMapper;

    public CPUserImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public CPUserBO getById(long id) {
        return userMapper.selectById(id).toUserBO();
    }

    @Override
    public boolean removeById(long id) {
        userMapper.deleteById(id);
        return true;
    }

    @Override
    public boolean register(CPUserBO user, String password) {
        UserPO userPO = new UserPO();
        userPO.fillData(user,password);
        userMapper.insert(userPO);
        return true;
    }

    @Override
    public boolean update(CPUserBO user, String password) {
        UserPO userPO = new UserPO();
        userPO.fillData(user,password);
        if(password.isEmpty()){
            userPO.setPassword(userMapper.selectById(user.getId()).getPassword());
        }
        userMapper.updateById(userPO);
        return true;
    }

    @Override
    public CPUserBO login(String email, String password) {
        QueryWrapper<UserPO> userPOQueryWrapper = new QueryWrapper<>();
        userPOQueryWrapper.eq("email",email);
        userPOQueryWrapper.eq("password",password);
        UserPO userPO = userMapper.selectOne(userPOQueryWrapper);
        if(userPO==null){
            return null;
        }
        return userPO.toUserBO();
    }
}
