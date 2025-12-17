package team.carrypigeon.backend.api.bo.connection;

/**
 * 与连接本身相关的会话属性 key 定义。
 * <p>
 * 这些属性由连接层（如 Netty）在 {@link CPSession} 上写入，
 * 上层业务可以通过统一的 key 从 {@link CPSession#getAttributeValue(String, Class)} 中读取。
 */
public final class CPConnectionAttributes {

    private CPConnectionAttributes() {
    }

    /**
     * 原始远端地址字符串，例如 "127.0.0.1:12345"。
     */
    public static final String REMOTE_ADDRESS = "ConnectionRemoteAddress";

    /**
     * 远端 IP 字符串，例如 "127.0.0.1"。
     */
    public static final String REMOTE_IP = "ConnectionRemoteIp";

    /**
     * 远端端口号，Integer。
     */
    public static final String REMOTE_PORT = "ConnectionRemotePort";
}

