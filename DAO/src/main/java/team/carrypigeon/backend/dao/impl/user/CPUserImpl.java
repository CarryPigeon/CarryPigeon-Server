package team.carrypigeon.backend.dao.impl.user;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.dao.mapper.key.KeyMapper;
import team.carrypigeon.backend.dao.mapper.key.KeyPO;
import team.carrypigeon.backend.dao.mapper.user.UserMapper;
import team.carrypigeon.backend.dao.mapper.user.UserPO;

import java.time.LocalDateTime;

@Component
@Slf4j
public class CPUserImpl implements CPUserDAO {

    private final UserMapper userMapper;

    private final KeyMapper keyMapper;

    public CPUserImpl(UserMapper userMapper, KeyMapper keyMapper) {
        this.userMapper = userMapper;
        this.keyMapper = keyMapper;
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
        // 判断是否存在相同邮箱
        QueryWrapper<UserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email",user.getEmail());
        if(userMapper.selectOne(queryWrapper)!=null){
            return false;
        }
        userMapper.insert(userPO);
        return true;
    }

    @Override
    public boolean update(CPUserBO user, String password) {
        UserPO userPO = new UserPO();
        userPO.fillData(user,password);
        if(password==null||password.isEmpty()){
            userPO.setPassword(userMapper.selectById(user.getId()).getPassword());
        }
        userMapper.updateById(userPO);
        return true;
    }

    @Override
    public String getKey(String email, String password) {
        // 校验是否存在用户
        QueryWrapper<UserPO> userPOQueryWrapper = new QueryWrapper<>();
        userPOQueryWrapper.eq("email",email);
        UserPO userPO = userMapper.selectOne(userPOQueryWrapper);
        if (userPO == null|| !BCrypt.checkpw(password,userPO.getPassword())){
            return "";
        }
        // 生成密钥
        String key = UUID.randomUUID().toString();
        keyMapper.insert(new KeyPO(IdUtil.generateId(),userPO.getId(),key, LocalDateTime.now()));
        return key;
    }


    @Override
    public CPUserBO login(String loginKey) {
        QueryWrapper<KeyPO> keyPOQueryWrapper = new QueryWrapper<>();
        keyPOQueryWrapper.eq("login_key",loginKey);
        KeyPO keyPO = keyMapper.selectOne(keyPOQueryWrapper);
        if (keyPO == null) {
            return null;
        }
        return userMapper.selectById(keyPO.getUid()).toUserBO();
    }
}
