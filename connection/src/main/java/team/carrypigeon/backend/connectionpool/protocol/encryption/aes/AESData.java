package team.carrypigeon.backend.connectionpool.protocol.encryption.aes;

/**
 * AES加密数据载体<br/>
 * 包含密文和随机数向量
 * @author midreamsheep
 * */
public record AESData(byte[] ciphertext, byte[] nonce) {}
