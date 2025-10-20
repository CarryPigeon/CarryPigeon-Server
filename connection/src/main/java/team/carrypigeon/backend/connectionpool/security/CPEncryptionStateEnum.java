package team.carrypigeon.backend.connectionpool.security;

/**
 * 用于校验用户状态
 * */
public enum CPEncryptionStateEnum {
    WAIT_ASYMMETRY,///等待用户发送公钥解密
    WAIT_VERIFICATION,///  等待用户发送密钥加密后的verification
    SUCCESS/// 连接建立成功，正式开始通讯
}
