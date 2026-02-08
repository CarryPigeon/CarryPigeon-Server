package team.carrypigeon.backend.chat.domain.attribute;

/**
 * Chat-Domain 会话属性 key（写入 {@code CPSession}）。
 * <p>
 * 这些属性由业务链路在登录成功后写入，会被登录校验节点读取。
 */
public final class CPChatDomainAttributes {

    /** {@code Long}: 当前连接绑定的登录用户 id。 */
    public static final String CHAT_DOMAIN_USER_ID = "ChatDomainUserId";

    private CPChatDomainAttributes() {
    }
}
