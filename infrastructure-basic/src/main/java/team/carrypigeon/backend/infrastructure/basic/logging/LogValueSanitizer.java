package team.carrypigeon.backend.infrastructure.basic.logging;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 日志值安全处理器。
 * 职责：把不可信文本收敛为有长度上限的单行值，并遮蔽查询字符串中的敏感参数。
 * 边界：只处理日志表示，不修改请求、响应或业务数据。
 */
public final class LogValueSanitizer {

    private static final int MAX_VALUE_LENGTH = 512;
    private static final String TRUNCATION_MARKER = "...";

    private LogValueSanitizer() {
    }

    /**
     * 把文本转换为安全的单行日志值。
     * 约束：ISO 控制字符替换为空格，首尾空白被移除，超出上限的内容以省略标记截断。
     *
     * @param value 原始日志值
     * @return 可安全写入单行日志的文本；空值返回空字符串
     */
    public static String singleLine(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int inputLimit = Math.min(value.length(), MAX_VALUE_LENGTH + 1);
        StringBuilder sanitized = new StringBuilder(inputLimit);
        for (int index = 0; index < inputLimit; index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character)) {
                if (sanitized.isEmpty() || sanitized.charAt(sanitized.length() - 1) != ' ') {
                    sanitized.append(' ');
                }
            } else {
                sanitized.append(character);
            }
        }
        String result = sanitized.toString().trim();
        if (value.length() <= MAX_VALUE_LENGTH && result.length() <= MAX_VALUE_LENGTH) {
            return result;
        }
        int contentLength = MAX_VALUE_LENGTH - TRUNCATION_MARKER.length();
        int endIndex = Math.min(result.length(), contentLength);
        if (endIndex > 0 && endIndex < result.length()
                && Character.isHighSurrogate(result.charAt(endIndex - 1))
                && Character.isLowSurrogate(result.charAt(endIndex))) {
            endIndex--;
        }
        return result.substring(0, endIndex).trim() + TRUNCATION_MARKER;
    }

    /**
     * 遮蔽查询字符串中的凭证类参数并限制最终日志长度。
     * 语义：参数名会先按 UTF-8 URL 解码再判断，避免编码后的敏感名称绕过遮蔽；畸形编码采用保守遮蔽。
     *
     * @param query 原始查询字符串，不包含问号
     * @return 可安全记录的查询字符串
     */
    public static String query(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(Math.min(query.length(), MAX_VALUE_LENGTH));
        int cursor = 0;
        boolean firstParameter = true;
        while (cursor <= query.length()) {
            int separator = query.indexOf('&', cursor);
            int parameterEnd = separator < 0 ? query.length() : separator;
            int boundedEnd = Math.min(parameterEnd, cursor + MAX_VALUE_LENGTH + 1);
            if (!firstParameter) {
                sanitized.append('&');
            }
            firstParameter = false;
            appendSanitizedParameter(sanitized, query.substring(cursor, boundedEnd));
            if (sanitized.length() > MAX_VALUE_LENGTH || separator < 0) {
                break;
            }
            cursor = separator + 1;
        }
        return singleLine(sanitized.toString());
    }

    /**
     * 清理 URI，并对其中的查询字符串执行敏感参数遮蔽。
     *
     * @param uri 原始 URI
     * @return 可安全记录的 URI
     */
    public static String uri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        int queryIndex = uri.indexOf('?');
        if (queryIndex < 0) {
            return singleLine(uri);
        }
        String path = singleLine(uri.substring(0, queryIndex));
        String query = query(uri.substring(queryIndex + 1));
        return singleLine(path + "?" + query);
    }

    private static void appendSanitizedParameter(StringBuilder target, String parameter) {
        int valueSeparator = parameter.indexOf('=');
        if (valueSeparator < 0) {
            target.append(singleLine(parameter));
            return;
        }
        String rawName = parameter.substring(0, valueSeparator);
        String rawValue = parameter.substring(valueSeparator + 1);
        target.append(singleLine(rawName)).append('=');
        String decodedName;
        try {
            decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            target.append("***");
            return;
        }
        target.append(isSensitiveParameter(decodedName) ? "***" : singleLine(rawValue));
    }

    private static boolean isSensitiveParameter(String parameterName) {
        String normalized = parameterName.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.equals("authorization")
                || normalized.equals("api_key")
                || normalized.equals("apikey")
                || normalized.equals("code")
                || normalized.equals("password")
                || normalized.equals("secret")
                || normalized.equals("token")
                || normalized.endsWith("_code")
                || normalized.endsWith("_password")
                || normalized.endsWith("_secret")
                || normalized.endsWith("_token");
    }
}
