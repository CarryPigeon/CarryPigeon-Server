package team.carrypigeon.backend.dao.database.impl.user.token;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.dao.database.mapper.user.token.UserTokenMapper;
import team.carrypigeon.backend.dao.database.mapper.user.token.UserTokenPO;

import java.util.Optional;

@Service
public class UserTokenDaoImpl implements UserTokenDao {

    private final UserTokenMapper userTokenMapper;

    public UserTokenDaoImpl(UserTokenMapper userTokenMapper) {
        this.userTokenMapper = userTokenMapper;
    }

    @Override
    public CPUserToken getById(long id) {
        return Optional.ofNullable(userTokenMapper.selectById(id)).map(UserTokenPO::toBo).orElse(null);
    }

    @Override
    public CPUserToken[] getByUserId(long userId) {
        LambdaQueryWrapper<UserTokenPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTokenPO::getUid, userId);
        return userTokenMapper.selectList(queryWrapper).stream().map(UserTokenPO::toBo).toArray(CPUserToken[]::new);
    }

    @Override
    public CPUserToken getByToken(String token) {
        LambdaQueryWrapper<UserTokenPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTokenPO::getToken, token);
        return Optional.ofNullable(userTokenMapper.selectOne(queryWrapper)).map(UserTokenPO::toBo).orElse(null);
    }

    @Override
    public boolean save(CPUserToken token) {
        return userTokenMapper.insertOrUpdate(UserTokenPO.from(token));
    }

    @Override
    public boolean delete(String token) {
        LambdaQueryWrapper<UserTokenPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTokenPO::getToken, token);
        return userTokenMapper.delete(queryWrapper) != 0;
    }
}
