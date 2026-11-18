package team.carrypigeon.backend.chat.domain.features.message.domain.capability;

import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.input.MessageChangeCursor;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result.MessageChangePage;

/** 读取频道短期消息变更；调用方负责检查账号的频道访问权限。 */
public interface MessageChangeReader {

    /**
     * 读取游标之后的变更，或在游标为空时返回当前事件位置。
     * 游标过期或记录不再完整时抛出消息变更游标过期的业务异常。
     *
     * @param channelId 目标频道 ID
     * @param cursor 上次返回的游标；null 表示获取当前事件位置
     * @param limit 最大返回条数，须为正数
     * @return 事件及下次读取位置
     */
    MessageChangePage read(long channelId, MessageChangeCursor cursor, int limit);
}
