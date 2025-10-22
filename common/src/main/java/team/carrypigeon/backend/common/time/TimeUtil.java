package team.carrypigeon.backend.common.time;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeUtil {
    public static long getCurrentTime(){
        return System.currentTimeMillis();
    }

    public static LocalDateTime getCurrentLocalTime(){
        return LocalDateTime.now();
    }

    public static long LocalDateTimeToMillis(LocalDateTime localDateTime){
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static LocalDateTime MillisToLocalDateTime(long millis){
        return LocalDateTimeUtil.of(millis);
    }
}
