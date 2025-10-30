package team.carrypigeon.backend.dao.database.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.dao.database.mapper.user.UserMapper;
import team.carrypigeon.backend.dao.database.mapper.user.UserPO;

import java.util.Optional;

@Service
public class UserDaoImpl implements UserDao {

    private final UserMapper userMapper;

    public UserDaoImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public CPUser getById(long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(UserPO::toBo).orElse(null);
    }

    @Override
    public CPUser getByEmail(String email) {
        LambdaQueryWrapper<UserPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPO::getEmail, email);
        return Optional.ofNullable(userMapper.selectOne(queryWrapper)).map(UserPO::toBo).orElse(null);
    }

    @Override
    public boolean save(CPUser user) {
        return userMapper.insertOrUpdate(UserPO.fromBo(user));
    }
}
