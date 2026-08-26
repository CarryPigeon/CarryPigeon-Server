package team.carrypigeon.backend.chat.domain.features.user.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetCurrentUserProfileQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfileByAccountIdQuery;

/**
 * 用户资料领域 API。
 * 职责：暴露当前用户资料、公开资料查询和资料更新能力。
 * 边界：不暴露 controller 协议、具体实现类和资料仓储细节。
 * 输入：用户资料命令、查询对象或账号 ID 列表等稳定业务入参。
 * 输出：用户资料投影、公开资料列表或更新副作用。
 * 失败语义：账号不存在、资料参数非法和查询条件非法由领域问题异常表达。
 * 调用方：通过本接口读取和维护用户资料，不直接访问资料仓储。
 */
public interface UserProfileApi {

    /**
     * 获取当前账号的用户资料。
     * 输入：查询对象携带当前账号上下文。
     * 输出：当前账号的用户资料投影。
     * 失败语义：账号资料不存在时返回领域问题。
     *
     * @param query 当前用户资料查询对象
     * @return 当前用户资料投影
     */
    UserProfileResult getCurrentUserProfile(GetCurrentUserProfileQuery query);

    /**
     * 按账号 ID 查询用户资料。
     * 输入：查询对象携带目标账号 ID。
     * 输出：目标账号的用户资料投影。
     * 约束：公开字段和可见性由领域实现控制。
     *
     * @param query 按账号 ID 查询用户资料的查询对象
     * @return 目标账号的用户资料投影
     */
    UserProfileResult getUserProfileByAccountId(GetUserProfileByAccountIdQuery query);

    /**
     * 批量查询公开用户资料。
     * 输入：目标账号 ID 列表。
     * 输出：公开用户资料投影列表。
     * 约束：不存在或不可公开的账号不应泄漏内部资料。
     *
     * @param accountIds 目标账号 ID 列表
     * @return 公开用户资料投影列表
     */
    List<UserProfileResult> getPublicUserProfiles(List<Long> accountIds);

    /**
     * 更新当前账号用户资料。
     * 输入：命令携带当前账号和待更新资料字段。
     * 输出：更新后的用户资料投影。
     * 约束：昵称、头像、简介等字段必须满足领域资料规则。
     *
     * @param command 当前用户资料更新业务命令
     * @return 更新后的用户资料投影
     */
    UserProfileResult updateCurrentUserProfile(UpdateCurrentUserProfileCommand command);

}
