package team.carrypigeon.backend.dao.database.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.dao.database.mapper.user.UserMapper;
import team.carrypigeon.backend.dao.database.mapper.user.UserPO;

import java.util.Optional;

@Slf4j
@Service
public class UserDaoImpl implements UserDao {

    private final UserMapper userMapper;

    public UserDaoImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Cacheable(cacheNames = "userById", key = "#id", unless = "#result == null")
    public CPUser getById(long id) {
        log.debug("UserDaoImpl#getById - id={}", id);
        CPUser result = Optional.ofNullable(userMapper.selectById(id))
                .map(UserPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("UserDaoImpl#getById - user not found, id={}", id);
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "userByEmail", key = "#email", unless = "#result == null")
    public CPUser getByEmail(String email) {
        log.debug("UserDaoImpl#getByEmail - email={}", email);
        LambdaQueryWrapper<UserPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPO::getEmail, email);
        CPUser result = Optional.ofNullable(userMapper.selectOne(queryWrapper))
                .map(UserPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("UserDaoImpl#getByEmail - user not found, email={}", email);
        }
        return result;
    }

    @Override
    @CacheEvict(cacheNames = {"userById", "userByEmail"}, allEntries = true)
    public boolean save(CPUser user) {
        if (user == null) {
            log.error("UserDaoImpl#save called with null user");
            return false;
        }
        boolean success = userMapper.insertOrUpdate(UserPO.fromBo(user));
        if (success) {
            log.debug("UserDaoImpl#save success, uid={}", user.getId());
        } else {
            log.warn("UserDaoImpl#save failed, uid={}", user.getId());
        }
        return success;
    }
}
