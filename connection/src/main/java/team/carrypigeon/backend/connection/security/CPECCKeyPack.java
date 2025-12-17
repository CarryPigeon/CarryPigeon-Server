package team.carrypigeon.backend.connection.security;

/**
 * 兼容保留的 ECC 公钥包结构。
 * <p>
 * 当前握手流程已经改为由客户端使用服务端公钥加密 AES 密钥，
 * 不再需要在握手中上传客户端公钥，此类仅为向后兼容保留。
 */
public class CPECCKeyPack {

    private long id;
    private String key;

    public CPECCKeyPack() {
    }

    public CPECCKeyPack(long id, String key) {
        this.id = id;
        this.key = key;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
