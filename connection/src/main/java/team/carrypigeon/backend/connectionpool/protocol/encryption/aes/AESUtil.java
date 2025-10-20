package team.carrypigeon.backend.connectionpool.protocol.encryption.aes;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密工具类
 * @author midreamsheep
 * */
@Slf4j
public class AESUtil {
    // 加密算法:AES_GCM
    private static final String ALGORITHM_AES = "AES";
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    // 密钥长度
    private static final int KEY_LENGTH_BIT = 128;
    // GCM认证标签长度
    private static final int TAG_LENGTH_BIT = 128;
    // 初始向量大小
    private static final int IV_LENGTH_BYTE = 12;

    /**
     * 生成AES 128为密钥
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
            keyGen.init(KEY_LENGTH_BIT); // 128位密钥
            return keyGen.generateKey();
        }catch (NoSuchAlgorithmException e){
            log.error("unexpected Exception:{}",e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }
    /**
     * 带关联数据的加密
     * @param plaintext 明文
     * @param aad 关联数据
     *
     */
    public static AESData encryptWithAAD(String plaintext, byte[] aad, byte[] keyBytes) throws Exception {
        byte[] nonce = new byte[IV_LENGTH_BYTE];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, ALGORITHM_AES), spec);

        // 添加关联数据（不加密但参与认证）
        if (aad != null) {
            cipher.updateAAD(aad);
        }

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
        return new AESData(ciphertext, nonce);
    }

    /**
     * 带关联数据的解密
     */
    public static String decryptWithAAD(byte[] ciphertext, byte[] nonce, byte[] aad, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        if (aad != null) {
            cipher.updateAAD(aad);
        }

        return new String(cipher.doFinal(ciphertext));
    }
}
