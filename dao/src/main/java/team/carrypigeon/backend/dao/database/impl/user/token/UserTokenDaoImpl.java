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

/**
 * {@link UserTokenDao} 的数据库实现（MyBatis-Plus + Spring Cache）。
 * <p>
 * 安全约束：token 属于敏感信息，日志中只允许输出长度或脱敏摘要。
 */
@Slf4j
@Service
public class UserTokenDaoImpl implements UserTokenDao {

    private final UserTokenMapper userTokenMapper;

    /**
     * 创建用户 token DAO 实现（由 Spring 注入 {@link UserTokenMapper}）。
     */
    public UserTokenDaoImpl(UserTokenMapper userTokenMapper) {
        this.userTokenMapper = userTokenMapper;
    }

    /**
     * 按令牌查询用户令牌。
     *
     * @param token 访问令牌字符串
     * @return 匹配的令牌实体；不存在时返回 {@code null}
     */
    @Override
    @Cacheable(cacheNames = "userTokenByToken", key = "#token", unless = "#result == null")
    public CPUserToken getByToken(String token) {
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

    /**
     * 按主键查询数据。
     *
     * @param id 令牌记录 ID
     * @return 匹配的令牌实体；不存在时返回 {@code null}
     */
    @Override
    @Cacheable(cacheNames = "userTokenByUid", key = "#id", unless = "#result == null")
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

    /**
     * 按用户查询令牌列表。
     *
     * @param uid 用户 ID
     * @return 用户名下的令牌数组
     */
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

    /**
     * 保存用户令牌数据。
     *
     * @param userToken 待保存的令牌实体
     * @return {@code true} 表示写库成功
     */
    @Override
    @CacheEvict(cacheNames = {"userTokenByToken","userTokenByUid"}, allEntries = true)
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

    /**
     * 删除用户令牌数据。
     *
     * @param userToken 待删除的令牌实体
     * @return {@code true} 表示删除成功
     */
    @Override
    @CacheEvict(cacheNames = {"userTokenByToken","userTokenByUid"}, allEntries = true)
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
