package team.carrypigeon.backend.chat.domain.features.user.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfilePageResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.SearchUserProfilesQuery;

/**
 * 用户资料领域 API。
 * 职责：暴露当前用户资料、公开资料列表、分页查询、搜索和资料更新能力。
 * 边界：不暴露 controller 协议、具体实现类和资料仓储细节。
 * 输入：用户资料命令、查询对象或账号 ID 列表等稳定业务入参。
 * 输出：用户资料投影、分页投影、邮箱字符串或更新副作用。
 * 失败语义：账号不存在、资料参数非法、邮箱更新非法和查询条件非法由领域问题异常表达。
 * 调用方：通过本接口读取和维护用户资料，不直接访问资料仓储。
 */
public interface UserProfileApi {

    /**
     * 获取当前账号的用户资料。
     * 输入：命令携带当前账号上下文。
     * 输出：当前账号的用户资料投影。
     * 失败语义：账号资料不存在时返回领域问题。
     *
     * @param command 当前用户资料读取业务命令
     * @return 当前用户资料投影
     */
    UserProfileResult getCurrentUserProfile(GetCurrentUserProfileCommand command);

    /**
     * 获取当前账号邮箱。
     * 输入：当前账号 ID。
     * 输出：账号邮箱地址。
     * 边界：该能力用于领域内需要邮箱展示或校验的场景，不暴露认证凭证细节。
     *
     * @param accountId 当前账号 ID
     * @return 当前账号邮箱地址
     */
    String getCurrentUserEmail(long accountId);

    /**
     * 按账号 ID 查询用户资料。
     * 输入：查询命令携带访问账号和目标账号。
     * 输出：目标账号的用户资料投影。
     * 约束：公开字段和可见性由领域实现控制。
     *
     * @param command 按账号 ID 查询用户资料的业务命令
     * @return 目标账号的用户资料投影
     */
    UserProfileResult getUserProfileByAccountId(GetUserProfileByAccountIdCommand command);

    /**
     * 查询当前账号可见的用户资料列表。
     * 输入：当前账号 ID。
     * 输出：用户资料投影列表。
     * 约束：列表范围由领域可见性规则控制。
     *
     * @param accountId 当前账号 ID
     * @return 当前账号可见的用户资料投影列表
     */
    List<UserProfileResult> listUserProfiles(long accountId);

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
     * 分页查询用户资料。
     * 输入：查询对象携带当前账号、游标、数量和筛选条件。
     * 输出：用户资料分页投影。
     * 约束：分页游标和可见性由领域实现统一校验。
     *
     * @param query 用户资料分页查询条件
     * @return 用户资料分页投影
     */
    UserProfilePageResult getUserProfiles(GetUserProfilesQuery query);

    /**
     * 搜索用户资料。
     * 输入：查询对象携带当前账号、关键字、游标和分页数量。
     * 输出：用户资料搜索分页投影。
     * 约束：搜索关键字长度、可见性和排序规则由领域实现控制。
     *
     * @param query 用户资料搜索查询条件
     * @return 用户资料搜索分页投影
     */
    UserProfilePageResult searchUserProfiles(SearchUserProfilesQuery query);

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

    /**
     * 更新当前账号邮箱。
     * 输入：当前账号 ID 和新邮箱地址。
     * 副作用：持久化账号资料中的邮箱信息。
     * 约束：邮箱格式和唯一性由领域实现校验。
     *
     * @param accountId 当前账号 ID
     * @param email 新邮箱地址
     */
    void updateCurrentUserEmail(long accountId, String email);
}
