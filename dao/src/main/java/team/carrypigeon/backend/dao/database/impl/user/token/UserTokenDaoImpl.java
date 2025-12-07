package team.carrypigeon.backend.dao.database.impl.user.token;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.dao.database.mapper.user.token.UserTokenMapper;
import team.carrypigeon.backend.dao.database.mapper.user.token.UserTokenPO;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserTokenDaoImpl implements UserTokenDao {

    private final UserTokenMapper userTokenMapper;

    public UserTokenDaoImpl(UserTokenMapper userTokenMapper) {
        this.userTokenMapper = userTokenMapper;
    }

    @Override
    @Cacheable(cacheNames = "userTokenByToken", key = "#token")
    public CPUserToken getByToken(String token) {
        // 为避免泄露敏感信息，这里不直接打印 token 内容，仅记录长度
        log.debug("UserTokenDaoImpl#getByToken - tokenLength={}", token == null ? 0 : token.length());
        LambdaQueryWrapper<UserTokenPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTokenPO::getToken,token);
        CPUserToken result = Optional.ofNullable(userTokenMapper.selectOne(queryWrapper))
                .map(UserTokenPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("UserTokenDaoImpl#getByToken - token not found");
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "userTokenByUid", key = "#id")
    public CPUserToken getById(long id) {
        log.debug("UserTokenDaoImpl#getById - id={}", id);
        CPUserToken result = Optional.ofNullable(userTokenMapper.selectById(id))
                .map(UserTokenPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("UserTokenDaoImpl#getById - token not found, id={}", id);
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "userTokenByUid", key = "#uid")
    public CPUserToken[] getByUserId(long uid) {
        log.debug("UserTokenDaoImpl#getByUserId - uid={}", uid);
        LambdaQueryWrapper<UserTokenPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTokenPO::getUid,uid);
        List<UserTokenPO> list = userTokenMapper.selectList(queryWrapper);
        CPUserToken[] result = list.stream()
                .map(UserTokenPO::toBo)
                .toArray(CPUserToken[]::new);
        log.debug("UserTokenDaoImpl#getByUserId - resultCount={}, uid={}", result.length, uid);
        return result;
    }

    @Override
    @CacheEvict(cacheNames = {"userTokenByToken","userTokenByUid","userTokenByToken"}, allEntries = true)
    public boolean save(CPUserToken userToken) {
        if (userToken == null) {
            log.error("UserTokenDaoImpl#save called with null userToken");
            return false;
        }
        boolean success = userTokenMapper.insertOrUpdate(UserTokenPO.from(userToken));
        if (success) {
            log.debug("UserTokenDaoImpl#save success, id={}, uid={}", userToken.getId(), userToken.getUid());
        } else {
            log.warn("UserTokenDaoImpl#save failed, id={}, uid={}", userToken.getId(), userToken.getUid());
        }
        return success;
    }

    @Override
    @CacheEvict(cacheNames = {"userTokenByToken","userTokenByUid","userTokenByToken"}, allEntries = true)
    public boolean delete(CPUserToken userToken) {
        if (userToken == null) {
            log.error("UserTokenDaoImpl#delete called with null userToken");
            return false;
        }
        boolean success = userTokenMapper.deleteById(userToken.getId())!=0;
        if (success) {
            log.debug("UserTokenDaoImpl#delete success, id={}", userToken.getId());
        } else {
            log.warn("UserTokenDaoImpl#delete failed, id={}", userToken.getId());
        }
        return success;
    }
}
