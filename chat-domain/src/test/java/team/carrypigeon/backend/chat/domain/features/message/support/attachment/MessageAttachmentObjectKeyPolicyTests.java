package team.carrypigeon.backend.chat.domain.features.message.support.attachment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageAttachmentObjectKeyPolicy 契约测试。
 * 职责：验证附件 objectKey 的统一构建规则和发送者范围判断规则。
 * 边界：只验证本地规则，不涉及对象存储访问。
 */
@Tag("unit")
class MessageAttachmentObjectKeyPolicyTests {

    /**
     * 验证 objectKey 会保持既有 canonical 格式并对文件名做兼容清洗。
     */
    @Test
    @DisplayName("build object key keeps canonical attachment format")
    void buildObjectKey_keepsCanonicalAttachmentFormat() {
        MessageAttachmentObjectKeyPolicy policy = new MessageAttachmentObjectKeyPolicy();

        String objectKey = policy.buildObjectKey(1L, "file", 1001L, 5001L, "demo report(1).pdf");

        assertEquals("channels/1/messages/file/accounts/1001/5001-demo_report_1_.pdf", objectKey);
    }

    /**
     * 验证发送者范围判断会要求频道、消息类型和发送者账户都匹配。
     */
    @Test
    @DisplayName("sender scope check matches canonical sender prefix")
    void isWithinSenderScope_matchingPrefix_returnsExpectedResult() {
        MessageAttachmentObjectKeyPolicy policy = new MessageAttachmentObjectKeyPolicy();

        assertTrue(policy.isWithinSenderScope(
                1L,
                "voice",
                1001L,
                "channels/1/messages/voice/accounts/1001/5001-demo.mp3"
        ));
        assertFalse(policy.isWithinSenderScope(
                1L,
                "voice",
                1001L,
                "channels/1/messages/voice/accounts/1002/5001-demo.mp3"
        ));
    }
}
