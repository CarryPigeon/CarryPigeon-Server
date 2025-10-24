package team.carrypigeon.backend.api.dao.database.channel.member;

import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;

/**
 * 频道成员数据库操作接口
 * @author midreamsheep
 * */
public interface ChannelMemberDao {
    /**
     * 通过id获取成员表
     * */
    CPChannelMember getById(long id);
    /**
     * 获取通道里的所有成员信息
     * @param cid 通道id
     * */
    CPChannelMember[] getAllMember(long cid);
    /**
     * 获取用户的成员信息，多条消息则获取最新的一条
     * @param uid 用户id
     * @param cid 通道id
     * */
    CPChannelMember getMember(long uid, long cid);
    /**
     * 获取用户的所有成员信息
     * @param uid 用户id
     * */
    CPChannelMember[] getAllMemberByUserId(long uid);
    /**
     * 保存成员信息表
     * @param channelMember 成员信息表
     * */
    boolean save(CPChannelMember channelMember);

    /**
     * 删除成员信息表
     * @param cpChannelMember 成员信息表
     * */
    boolean delete(CPChannelMember cpChannelMember);
}