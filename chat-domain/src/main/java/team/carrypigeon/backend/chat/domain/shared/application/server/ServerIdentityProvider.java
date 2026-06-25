package team.carrypigeon.backend.chat.domain.shared.application.server;

/**
 * 服务端身份提供器。
 * 职责：向 chat-domain 各 feature 暴露稳定的当前服务端标识。
 * 边界：这里只提供只读身份值，不承担配置解析或节点协调逻辑。
 */
public interface ServerIdentityProvider {

    /**
     * 返回当前服务端稳定 ID。
     *
     * @return 当前服务端 ID
     */
    String id();
}
