package team.carrypigeon.backend.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration
/**
 * Jackson 统一配置。
 * <p>
 * 该配置为全局提供 {@link ObjectMapper}，用于 TCP 协议（CPPacket/CPResponse/通知）与 Web API 的 JSON 读写。
 * </p>
 */
public class JacksonConfiguration {

    /**
     * 提供项目统一的 {@link ObjectMapper}。
     * <ul>
     *     <li>对外 JSON 命名策略：{@code snake_case}</li>
     *     <li>忽略未知字段：便于向前/向后兼容</li>
     *     <li>日期时间：支持 {@code java.time}（{@link JavaTimeModule}）并使用统一格式</li>
     *     <li>禁止将 {@code null} 写入属性：默认 fail-fast（{@link Nulls#FAIL}）</li>
     * </ul>
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        //在反序列化时忽略在 json 中存在但 Java 对象不存在的属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //在序列化时日期格式默认为 yyyy-MM-dd'T'HH:mm:ss.SSSZ
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        //在序列化时自定义时间日期格式
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 实现下划线格式到驼峰格式的自动转化
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        // 默认转换非空
        mapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.FAIL));
        return mapper;
    }
}
