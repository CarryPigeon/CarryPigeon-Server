package team.carrypigeon.backend.infrastructure.basic.id;

/**
 * ID 相关基础工具。
 * 职责：提供雪花 ID 的基础转换能力，避免业务代码重复处理 ID 文本格式。
 */
public final class Ids {

    private Ids() {
    }

    public static String toString(long id) {
        return String.valueOf(id);
    }

    public static long parse(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return Long.parseLong(id);
    }
}
