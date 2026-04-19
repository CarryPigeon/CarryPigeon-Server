package team.carrypigeon.backend.infrastructure.basic.id;

/**
 * 统一 ID 生成抽象。
 * 职责：为项目提供稳定的 ID 生成入口。
 * 边界：这里只定义基础生成能力，不绑定具体业务对象语义。
 */
public interface IdGenerator {

    /**
     * @return 下一个 long 类型 ID
     */
    long nextLongId();

    /**
     * @return 下一个字符串形式 ID
     */
    default String nextStringId() {
        return String.valueOf(nextLongId());
    }
}
