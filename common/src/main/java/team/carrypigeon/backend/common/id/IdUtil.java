package team.carrypigeon.backend.common.id;

import cn.hutool.core.codec.Base64;

public class IdUtil {
    public static long generateId(){
        return cn.hutool.core.util.IdUtil.getSnowflake().nextId();
    }

    public static String generateToken(){
        return Base64.encode(generateId()+"");
    }
}
