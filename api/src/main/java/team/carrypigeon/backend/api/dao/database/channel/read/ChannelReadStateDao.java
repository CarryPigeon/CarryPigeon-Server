package team.carrypigeon.backend.api.dao.database.channel.read;

import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;

/**
 * DAO interface for channel message read state.
 * <p>
 * One record represents the read state of a user in a specific channel.
 */
public interface ChannelReadStateDao {

    /**
     * Get read state by primary id.
     */
    CPChannelReadState getById(long id);

    /**
     * Get read state by user id and channel id.
     *
     * @param uid user id
     * @param cid channel id
     */
    CPChannelReadState getByUidAndCid(long uid, long cid);

    /**
     * Save read state (insert or update).
     *
     * @param state read state entity
     * @return true if success
     */
    boolean save(CPChannelReadState state);

    /**
     * Delete read state record.
     *
     * @param state read state entity
     * @return true if success
     */
    boolean delete(CPChannelReadState state);
}

