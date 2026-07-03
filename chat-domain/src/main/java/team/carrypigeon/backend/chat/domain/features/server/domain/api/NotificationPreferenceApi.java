package team.carrypigeon.backend.chat.domain.features.server.domain.api;

import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationChannelPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationServerPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.NotificationPreferencesResult;

/**
 * 通知偏好领域 API。
 * 职责：暴露通知偏好查询与更新能力。
 * 边界：不暴露 controller 协议、具体实现类和偏好仓储细节。
 * 输入：账号 ID 或通知偏好更新命令对象。
 * 输出：通知偏好投影或更新副作用。
 * 失败语义：账号非法、频道不可访问或偏好值非法由领域问题异常表达。
 * 调用方：通过本接口读取和修改通知偏好，不直接写偏好存储。
 */
public interface NotificationPreferenceApi {

    /**
     * 查询账号的通知偏好。
     * 输入：当前账号 ID。
     * 输出：服务级和频道级通知偏好投影。
     * 约束：缺省偏好和显式偏好合并规则由领域实现负责。
     *
     * @param accountId 当前账号 ID
     * @return 当前账号的通知偏好投影
     */
    NotificationPreferencesResult getNotificationPreferences(long accountId);

    /**
     * 更新账号的服务级通知偏好。
     * 输入：命令携带账号和服务级通知偏好设置。
     * 副作用：持久化服务级通知偏好。
     * 约束：偏好取值必须属于领域允许集合。
     *
     * @param command 服务级通知偏好更新业务命令
     */
    void updateServerPreference(UpdateNotificationServerPreferenceCommand command);

    /**
     * 更新账号在指定频道的通知偏好。
     * 输入：命令携带账号、频道和频道级通知偏好设置。
     * 副作用：持久化频道级通知偏好。
     * 约束：账号必须有权限访问目标频道。
     *
     * @param command 频道级通知偏好更新业务命令
     */
    void updateChannelPreference(UpdateNotificationChannelPreferenceCommand command);
}
