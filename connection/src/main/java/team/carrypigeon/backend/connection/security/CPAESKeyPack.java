package team.carrypigeon.backend.connection.security;

/**
 * AES 会话密钥包：
 * <ul>
 *     <li>id: 客户端请求标识，用于日志关联；</li>
 *     <li>sessionId: 会话 id（可选，当前服务端不强依赖）；</li>
 *     <li>key: 使用服务端公钥加密后的 AES 密钥（Base64 文本再 Base64 封装）。</li>
 * </ul>
 */
public class CPAESKeyPack {

    private long id;
    private long sessionId;
    private String key;

    public CPAESKeyPack() {
    }

    public CPAESKeyPack(long id, long sessionId, String key) {
        this.id = id;
        this.sessionId = sessionId;
        this.key = key;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
