package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MailServiceProperties 契约测试。
 * 职责：验证邮件服务配置的默认值与关键约束。
 * 边界：只校验配置对象本身，不验证 Spring 绑定过程。
 */
@Tag("contract")
class MailServicePropertiesTests {

    /**
     * 验证默认构造会保持邮件服务关闭。
     */
    @Test
    @DisplayName("default constructor keeps mail disabled")
    void defaultConstructor_keepsMailDisabled() {
        MailServiceProperties properties = new MailServiceProperties();

        assertFalse(properties.enabled());
        assertEquals("", properties.fromAddress());
    }

    /**
     * 验证启用邮件服务时必须提供发件地址。
     */
    @Test
    @DisplayName("enabled mail without from address throws illegal argument")
    void enabledMail_withoutFromAddress_throwsIllegalArgument() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MailServiceProperties(true, "")
        );

        assertEquals("cp.infrastructure.service.mail.from-address must not be blank when mail is enabled", exception.getMessage());
    }
}
