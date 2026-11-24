package team.carrypigeon.backend.chat.domain.shared.id;

/**
 * ID 相关基础工具。
 * 职责：提供雪花 ID 的基础转换能力，避免业务代码重复处理 ID 文本格式。
 * 边界：作为无状态静态辅助能力直接暴露，不承担 ID 生成或注入职责。
 */
public final class IdUtil {

    private IdUtil() {
    }

    /**
     * 将数值 ID 转换为稳定字符串表示。
     *
     * @param id 数值 ID
     * @return 十进制字符串形式的 ID
     */
    public static String toString(long id) {
        return String.valueOf(id);
    }

    /**
     * 将十进制字符串解析为数值 ID。
     *
     * @param id 十进制字符串形式的 ID
     * @return 数值 ID
     * @throws IllegalArgumentException 输入为空白时抛出
     */
    public static long parse(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return Long.parseLong(id);
    }

    /**
     * 解析雪花ID
     * @param id 输入的字符串
     * @return 解析后的雪花ID
     * */
    public static long parseSnowId(String id){
        // 判断id是不是符合雪花ID的格式
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        long snowId = Long.parseLong(id);
        if (snowId<=0){
            throw new IllegalArgumentException("id must be greater than 0");
        }
        return snowId;
    }
}
