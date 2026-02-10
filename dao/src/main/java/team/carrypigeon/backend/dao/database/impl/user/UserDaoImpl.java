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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link UserDao} 的数据库实现（MyBatis-Plus + Spring Cache）。
 * <p>
 * 缓存策略：
 * <ul>
 *     <li>{@code userById}：按 id 缓存</li>
 *     <li>{@code userByEmail}：按 email 缓存</li>
 *     <li>写操作 {@link #save(CPUser)} 会清理相关缓存（allEntries=true）</li>
 * </ul>
 */
@Slf4j
@Service
public class UserDaoImpl implements UserDao {

    private final UserMapper userMapper;

    /**
     * 创建用户 DAO 实现（由 Spring 注入 {@link UserMapper}）。
     */
    public UserDaoImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 按主键查询数据。
     *
     * @param id 用户 ID
     * @return 匹配的用户对象；不存在时返回 {@code null}
     */
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

    /**
     * 按邮箱查询用户。
     *
     * @param email 用户邮箱地址
     * @return 匹配的用户对象；不存在时返回 {@code null}
     */
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

    /**
     * 保存用户数据。
     *
     * @param user 待保存的用户实体
     * @return {@code true} 表示写库成功
     */
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

    /**
     * 按主键列表批量查询数据。
     *
     * @param ids 用户 ID 集合
     * @return 命中的用户列表；当入参为空时返回空列表
     */
    @Override
    public List<CPUser> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<UserPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserPO::getId, ids);
        return userMapper.selectList(queryWrapper).stream()
                .map(UserPO::toBo)
                .toList();
    }
}
