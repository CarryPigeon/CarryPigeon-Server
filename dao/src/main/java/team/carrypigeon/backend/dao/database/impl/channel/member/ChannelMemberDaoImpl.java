package team.carrypigeon.backend.dao.database.impl.channel.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.dao.database.mapper.channel.member.ChannelMemberMapper;
import team.carrypigeon.backend.dao.database.mapper.channel.member.ChannelMemberPO;

import java.util.List;
import java.util.Optional;

/**
 * {@link ChannelMemberDao} 的数据库实现（MyBatis-Plus + Spring Cache）。
 * <p>
 * 写操作会全量失效成员相关缓存（allEntries=true），以避免组合 key 缓存不一致。
 */
@Slf4j
@Service
public class ChannelMemberDaoImpl implements ChannelMemberDao {

    private final ChannelMemberMapper channelMemberMapper;

    /**
     * 创建频道成员 DAO 实现（由 Spring 注入 {@link ChannelMemberMapper}）。
     */
    public ChannelMemberDaoImpl(ChannelMemberMapper channelMemberMapper) {
        this.channelMemberMapper = channelMemberMapper;
    }

    /**
     * 按主键查询数据。
     *
     * @param id 成员记录 ID
     * @return 匹配的成员记录；不存在时返回 {@code null}
     */
    @Override
    @Cacheable(cacheNames = "channelMemberById", key = "#id", unless = "#result == null")
    public CPChannelMember getById(long id) {
        log.debug("ChannelMemberDaoImpl#getById - id={}", id);
        CPChannelMember result = Optional.ofNullable(channelMemberMapper.selectById(id))
                .map(ChannelMemberPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelMemberDaoImpl#getById - member not found, id={}", id);
        }
        return result;
    }

    /**
     * 查询频道全部成员。
     *
     * @param cid 频道 ID
     * @return 该频道下的成员列表
     */
    @Override
    @Cacheable(cacheNames = "channelMembersByCid", key = "#cid")
    public CPChannelMember[] getAllMember(long cid) {
        log.debug("ChannelMemberDaoImpl#getAllMember - cid={}", cid);
        LambdaQueryWrapper<ChannelMemberPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelMemberPO::getCid, cid);
        List<ChannelMemberPO> channelMemberPOList = channelMemberMapper.selectList(queryWrapper);
        CPChannelMember[] result = channelMemberPOList.stream()
                .map(ChannelMemberPO::toBo)
                .toArray(CPChannelMember[]::new);
        log.debug("ChannelMemberDaoImpl#getAllMember - resultCount={}, cid={}", result.length, cid);
        return result;
    }

    /**
     * 查询频道指定成员。
     *
     * @param uid 用户 ID
     * @param cid 频道 ID
     * @return 匹配的成员记录；不存在时返回 {@code null}
     */
    @Override
    @Cacheable(cacheNames = "channelMemberByUidCid", key = "#uid + ':' + #cid", unless = "#result == null")
    public CPChannelMember getMember(long uid, long cid) {
        log.debug("ChannelMemberDaoImpl#getMember - uid={}, cid={}", uid, cid);
        LambdaQueryWrapper<ChannelMemberPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelMemberPO::getUid, uid).eq(ChannelMemberPO::getCid, cid);
        CPChannelMember result = Optional.ofNullable(channelMemberMapper.selectOne(queryWrapper))
                .map(ChannelMemberPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("ChannelMemberDaoImpl#getMember - member not found, uid={}, cid={}", uid, cid);
        }
        return result;
    }

    /**
     * 查询用户加入的全部频道成员记录。
     *
     * @param uid 用户 ID
     * @return 用户所在频道的成员记录数组
     */
    @Override
    @Cacheable(cacheNames = "channelMembersByUid", key = "#uid")
    public CPChannelMember[] getAllMemberByUserId(long uid) {
        log.debug("ChannelMemberDaoImpl#getAllMemberByUserId - uid={}", uid);
        LambdaQueryWrapper<ChannelMemberPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChannelMemberPO::getUid, uid);
        List<ChannelMemberPO> channelMemberPOList = channelMemberMapper.selectList(queryWrapper);
        CPChannelMember[] result = channelMemberPOList.stream()
                .map(ChannelMemberPO::toBo)
                .toArray(CPChannelMember[]::new);
        log.debug("ChannelMemberDaoImpl#getAllMemberByUserId - resultCount={}, uid={}", result.length, uid);
        return result;
    }

    /**
     * 保存频道成员记录。
     *
     * @param channelMember 待保存的成员实体
     * @return {@code true} 表示写库成功
     */
    @Override
    @CacheEvict(cacheNames = {"channelMemberById", "channelMembersByCid", "channelMemberByUidCid", "channelMembersByUid"}, allEntries = true)
    public boolean save(CPChannelMember channelMember) {
        if (channelMember == null) {
            log.error("ChannelMemberDaoImpl#save called with null channelMember");
            return false;
        }
        boolean success = channelMemberMapper.insertOrUpdate(ChannelMemberPO.from(channelMember));
        if (success) {
            log.debug("ChannelMemberDaoImpl#save success, id={}, uid={}, cid={}", channelMember.getId(), channelMember.getUid(), channelMember.getCid());
        } else {
            log.warn("ChannelMemberDaoImpl#save failed, id={}, uid={}, cid={}", channelMember.getId(), channelMember.getUid(), channelMember.getCid());
        }
        return success;
    }

    /**
     * 删除频道成员记录。
     *
     * @param cpChannelMember 待删除的成员实体
     * @return {@code true} 表示删除成功
     */
    @Override
    @CacheEvict(cacheNames = {"channelMemberById", "channelMembersByCid", "channelMemberByUidCid", "channelMembersByUid"}, allEntries = true)
    public boolean delete(CPChannelMember cpChannelMember) {
        if (cpChannelMember == null) {
            log.error("ChannelMemberDaoImpl#delete called with null channelMember");
            return false;
        }
        boolean success = channelMemberMapper.deleteById(cpChannelMember.getId())!=0;
        if (success) {
            log.debug("ChannelMemberDaoImpl#delete success, id={}", cpChannelMember.getId());
        } else {
            log.warn("ChannelMemberDaoImpl#delete failed, id={}", cpChannelMember.getId());
        }
        return success;
    }
}
