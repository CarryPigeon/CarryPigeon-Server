package team.carrypigeon.backend.connection.attribute;

import io.netty.util.AttributeKey;
import team.carrypigeon.backend.api.bo.connection.CPSession;

/**
 * 用于存储会话属性的key值与会话的AttributeKey
 * @author midreamsheep
 * */
public class ConnectionAttributes {
    // 用于netty通过上下文获取session
    public static final AttributeKey<CPSession> SESSIONS = AttributeKey.valueOf("SESSIONS");
    // 加密key值，主要用于保存aes的密钥，存储的数据为String格式(经过base64编码的aes-gcm密钥)
    public static final String ENCRYPTION_KEY = "EncryptionKey";
    // 加密状态，true为加密，false为未加密，存储的数据格式为 boolean
    public static final String ENCRYPTION_STATE = "EncryptionState";
    // 会话id，用于防止其他人的加密包攻击，存储的格式为 long
    public static final String PACKAGE_SESSION_ID = "SessionId";
    // 包序列id，用于防止重放攻击，存储的格式为 int
    public static final String PACKAGE_ID = "PackageId";
    // 本地包序列id，用于发送时标记包序号，每次使用需要进行自增，存储的格式为 int
    public static final String LOCAL_PACKAGE_ID = "LocalPackageId";
}
