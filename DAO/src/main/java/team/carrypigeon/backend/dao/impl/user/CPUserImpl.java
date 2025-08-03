package team.carrypigeon.backend.dao.impl.user;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.ognl.Token;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.dao.mapper.token.TokenMapper;
import team.carrypigeon.backend.dao.mapper.token.TokenPO;
import team.carrypigeon.backend.dao.mapper.user.UserMapper;
import team.carrypigeon.backend.dao.mapper.user.UserPO;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class CPUserImpl implements CPUserDAO {

    private final UserMapper userMapper;

    private final TokenMapper tokenMapper;

    public CPUserImpl(UserMapper userMapper, TokenMapper tokenMapper) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
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
    public String generateToken(String email, String password, String deviceName) {
        // 校验是否存在用户
        QueryWrapper<UserPO> userPOQueryWrapper = new QueryWrapper<>();
        userPOQueryWrapper.eq("email",email);
        UserPO userPO = userMapper.selectOne(userPOQueryWrapper);
        if (userPO == null|| !BCrypt.checkpw(password,userPO.getPassword())){
            return "";
        }
        // 检验有效token数量
        QueryWrapper<TokenPO> keyPOQueryWrapper = new QueryWrapper<>();
        keyPOQueryWrapper.eq("uid",userPO.getId());
        if (tokenMapper.selectCount(keyPOQueryWrapper)>=5){
            return "";
        }
        // 生成密钥
        String token = UUID.randomUUID().toString();
        // 对key进行加密
        token = BCrypt.hashpw(token,BCrypt.gensalt());
        tokenMapper.insert(new TokenPO(IdUtil.generateId(),userPO.getId(),token,deviceName, LocalDateTime.now()));
        return token;
    }

    @Override
    public String login(long uid, String token) {
        QueryWrapper<TokenPO> keyPOQueryWrapper = new QueryWrapper<>();
        keyPOQueryWrapper.eq("uid", uid);
        List<TokenPO> tokenPOS = tokenMapper.selectList(keyPOQueryWrapper);
        TokenPO targetTokenPO = null;
        for (TokenPO tokenPO : tokenPOS) {
            if (!BCrypt.checkpw(token, tokenPO.getToken())){
                continue;
            }
            // 判断是否过期
            if (!LocalDateTime.now().isAfter(tokenPO.getTime().plusDays(1))){
                return "";
            }
            // 如果过期了则重置token
            String string = UUID.randomUUID().toString();
            string = BCrypt.hashpw(string, BCrypt.gensalt());
            tokenPO.setToken(string);
            tokenPO.setTime(LocalDateTime.now());
            tokenMapper.updateById(tokenPO);
            targetTokenPO = tokenPO;
        }
        if (targetTokenPO == null) return null;
        // 判断有效token数是否超过最大值 最大值：TODO
        int count = 0;
        for (TokenPO tokenPO : tokenPOS) {
            if (!LocalDateTime.now().isAfter(tokenPO.getTime().plusDays(1))){
                count++;
            }
        }
        if (count >= 5){
            // 删除当前 token
            tokenMapper.deleteById(targetTokenPO.getId());
            return null;
        }
        // 登录成功
        return targetTokenPO.getToken();
    }
}