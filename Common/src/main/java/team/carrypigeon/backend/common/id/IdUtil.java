package team.carrypigeon.backend.common.id;

public class IdUtil {
    public static long generateId(){
        return cn.hutool.core.util.IdUtil.getSnowflake().nextId();
    }
}
