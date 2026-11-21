package team.carrypigeon.backend.chat.domain.features.message.controller.http.dto;

import java.io.IOException;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/** 消息 HTTP 请求中的正十进制字符串 ID。拒绝数值强制转换，避免截断小数或接受越界值。 */
public final class PositiveMessageApiIdDeserializer extends JsonDeserializer<Long> {

    private static final Pattern DECIMAL_ID = Pattern.compile("[0-9]+");

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText();
        if (parser.hasToken(JsonToken.VALUE_STRING) && DECIMAL_ID.matcher(value).matches()) {
            try {
                long id = Long.parseLong(value);
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // 超出 long 范围与其他格式错误使用相同的请求错误。
            }
        }
        throw InvalidFormatException.from(parser, "Expected a positive decimal string ID", value, Long.class);
    }
}
