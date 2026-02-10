package team.carrypigeon.backend.api.bo.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPSessionTests {

    @Test
    void defaultWrite_shouldDelegateToEncryptedWrite() {
        class RecordingSession implements CPSession {
            private String msg;
            private Boolean encrypted;

            /**
             * 记录测试输出。
             *
             * @param msg 待写出的消息文本
             * @param encrypted 是否按加密通道写出
             */
            @Override
            public void write(String msg, boolean encrypted) {
                this.msg = msg;
                this.encrypted = encrypted;
            }

            /**
             * 读取测试会话属性。
             *
             * @param key 属性键名
             * @param type 属性目标类型
             * @return 命中的属性值；当前测试实现固定返回 { null}
             */
            @Override
            public <T> T getAttributeValue(String key, Class<T> type) {
                return null;
            }

            /**
             * 写入测试会话属性。
             *
             * @param key 属性键名
             * @param value 待写入的属性值
             */
            @Override
            public void setAttributeValue(String key, Object value) {
            }

            /**
             * 关闭测试会话。
             */
            @Override
            public void close() {
            }
        }

        RecordingSession session = new RecordingSession();
        session.write("hello");
        assertEquals("hello", session.msg);
        assertEquals(true, session.encrypted);
    }
}

