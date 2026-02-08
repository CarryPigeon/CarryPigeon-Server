package team.carrypigeon.backend.api.dao.database.user;

import team.carrypigeon.backend.api.bo.domain.user.CPUser;

/**
 * User DAO.
 * <p>
 * This interface is part of the stable API surface. Business logic and plugins should depend on this module, not the
 * concrete database implementation.
 */
public interface UserDao {

    /**
     * Get a user by id.
     *
     * @param id user id
     */
    CPUser getById(long id);

    /**
     * Get a user by email.
     *
     * @param email user email
     */
    CPUser getByEmail(String email);

    /**
     * Save user (insert or update).
     *
     * @param user user entity
     */
    boolean save(CPUser user);

    /**
     * Batch load users by ids.
     * <p>
     * This method is used by HTTP APIs to avoid N+1 queries when rendering member/message lists.
     */
    java.util.List<CPUser> listByIds(java.util.Collection<Long> ids);
}
