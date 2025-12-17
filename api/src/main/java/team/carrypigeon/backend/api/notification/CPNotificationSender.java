package team.carrypigeon.backend.api.notification;

import team.carrypigeon.backend.api.connection.notification.CPNotification;

import java.util.Collection;

/**
 * 通知发送端口。
 * <p>
 * 通过该接口，可以向一组用户发送 {@link CPNotification} 通知，
 * 具体的发送实现由宿主系统在业务模块中提供。
 */
public interface CPNotificationSender {

    /**
     * 向指定用户集合发送通知。
     *
     * @param uids         用户 id 集合
     * @param notification 通知内容
     * @return true 表示发送流程正常完成（即便部分用户无在线会话）
     */
    boolean sendNotification(Collection<Long> uids, CPNotification notification);
}

