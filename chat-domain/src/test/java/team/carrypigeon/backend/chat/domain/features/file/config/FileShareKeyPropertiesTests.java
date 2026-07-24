package team.carrypigeon.backend.chat.domain.features.file.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FileShareKeyProperties 契约测试。
 * 职责：验证 file feature 自有 share key 密钥的最小强度边界。
 * 边界：不验证 auth JWT 配置或 share key 编解码算法。
 */
@Tag("unit")
class FileShareKeyPropertiesTests {

    /**
     * 验证满足最小长度的 file 密钥会原样保留。
     */
    @Test
    @DisplayName("constructor valid secret keeps configured value")
    void constructor_validSecret_keepsConfiguredValue() {
        String secret = "file-share-key-secret-at-least-32-characters";

        FileShareKeyProperties properties = new FileShareKeyProperties(secret);

        assertEquals(secret, properties.secret());
    }

    /**
     * 验证空 file 密钥会在配置对象创建阶段被拒绝。
     */
    @Test
    @DisplayName("constructor blank secret throws exception")
    void constructor_blankSecret_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileShareKeyProperties(" "));
    }

    /**
     * 验证弱 file 密钥会在配置对象创建阶段被拒绝。
     */
    @Test
    @DisplayName("constructor weak secret throws exception")
    void constructor_weakSecret_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileShareKeyProperties("short-secret"));
    }
}
