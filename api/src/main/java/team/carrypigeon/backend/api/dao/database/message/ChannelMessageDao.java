package team.carrypigeon.backend.api.dao.database.message;

import team.carrypigeon.backend.api.bo.domain.message.CPMessage;

/**
 * Channel message DAO.
 * <p>
 * This DAO intentionally uses {@code mid} (message id, snowflake long) as the stable cursor for pagination and unread
 * counting. Avoid using {@code send_time} as the cursor to prevent boundary bugs when multiple messages share the same
 * timestamp.
 */
public interface ChannelMessageDao {
    /**
     * Get a message by id.
     *
     * @param id message id
     */
    CPMessage getById(long id);
    /**
     * List messages in a channel before a cursor message id (exclusive), ordered by id desc.
     * <p>
     * Cursor semantics:
     * <ul>
     *   <li>cursorMid &lt;= 0: treated as {@link Long#MAX_VALUE} (first page)</li>
     *   <li>otherwise: list messages with {@code id &lt; cursorMid}</li>
     * </ul>
     *
     * @param cid       channel id
     * @param cursorMid cursor message id (exclusive)
     * @param count     max items, suggested range [1, 100]
     */
    CPMessage[] listBefore(long cid, long cursorMid, int count);

    /**
     * Count messages after a start message id (exclusive), in the given channel.
     *
     * @param cid      channel id
     * @param startMid last read message id (exclusive)
     */
    int countAfter(long cid, long startMid);

    /**
     * Save a message (insert or update).
     *
     * @param message message
     */
    boolean save(CPMessage message);

    /**
     * Delete a message.
     *
     * @param message message
     */
    boolean delete(CPMessage message);
}
