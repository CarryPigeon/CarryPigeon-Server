package team.carrypigeon.backend.dao.database.impl.user.token;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(cacheNames = "userTokenByToken", key = "#token")
    public CPUserToken getByToken(String token) {
        LambdaQueryWrapper<UserTokenPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTokenPO::getToken,token);
        return Optional.ofNullable(userTokenMapper.selectOne(queryWrapper)).map(UserTokenPO::toBo).orElse(null);
    }

    @Override
    @Cacheable(cacheNames = "userTokenByUid", key = "#id")
    public CPUserToken getById(long id) {
        return Optional.ofNullable(userTokenMapper.selectById(id)).map(UserTokenPO::toBo).orElse(null);
    }

    @Override
    @Cacheable(cacheNames = "userTokenByUid", key = "#uid")
    public CPUserToken[] getByUserId(long uid) {
        LambdaQueryWrapper<UserTokenPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTokenPO::getUid,uid);
        return userTokenMapper.selectList(queryWrapper).stream().map(UserTokenPO::toBo).toArray(CPUserToken[]::new);
    }

    @Override
    @CacheEvict(cacheNames = {"userTokenByToken","userTokenByUid","userTokenByToken"}, allEntries = true)
    public boolean save(CPUserToken userToken) {
        return userTokenMapper.insertOrUpdate(UserTokenPO.from(userToken));
    }

    @Override
    @CacheEvict(cacheNames = {"userTokenByToken","userTokenByUid","userTokenByToken"}, allEntries = true)
    public boolean delete(CPUserToken userToken) {
        return userTokenMapper.deleteById(userToken.getId())!=0;
    }
}
