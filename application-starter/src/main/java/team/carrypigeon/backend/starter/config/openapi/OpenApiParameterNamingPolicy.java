package team.carrypigeon.backend.starter.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAPI 参数命名策略。
 * 职责：把 path 模板以及非 header 参数名统一为 snake_case。
 * 边界：标准或既有 HTTP header 名保持原样，不改变 Spring MVC 的运行时路由。
 */
final class OpenApiParameterNamingPolicy {

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^}/]+)}");

    private OpenApiParameterNamingPolicy() {
    }

    /**
     * 规范化 OpenAPI 路径模板和操作参数。
     *
     * @param openApi 待规范化的 OpenAPI 文档
     */
    static void normalize(OpenAPI openApi) {
        if (openApi == null) {
            return;
        }
        if (openApi.getComponents() != null && openApi.getComponents().getParameters() != null) {
            normalizeParameters(new ArrayList<>(openApi.getComponents().getParameters().values()));
        }
        if (openApi.getPaths() == null) {
            return;
        }
        Map<String, PathItem> normalizedItems = new LinkedHashMap<>();
        openApi.getPaths().forEach((path, pathItem) -> {
            normalizeParameters(pathItem == null ? null : pathItem.getParameters());
            if (pathItem != null) {
                pathItem.readOperations().forEach(OpenApiParameterNamingPolicy::normalizeOperation);
            }
            String normalizedPath = normalizePath(path);
            if (normalizedItems.putIfAbsent(normalizedPath, pathItem) != null) {
                throw new IllegalStateException("OpenAPI path collision after snake_case normalization: "
                        + path + " -> " + normalizedPath);
            }
        });
        Paths normalizedPaths = new Paths();
        normalizedItems.forEach(normalizedPaths::addPathItem);
        openApi.setPaths(normalizedPaths);
    }

    private static void normalizeOperation(Operation operation) {
        if (operation != null) {
            normalizeParameters(operation.getParameters());
        }
    }

    private static void normalizeParameters(List<Parameter> parameters) {
        if (parameters == null) {
            return;
        }
        parameters.stream()
                .filter(parameter -> parameter != null && parameter.getName() != null)
                .filter(parameter -> !"header".equals(parameter.getIn()))
                .forEach(parameter -> parameter.setName(toSnakeCase(parameter.getName())));
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);
        StringBuilder normalized = new StringBuilder(path.length() + 8);
        while (matcher.find()) {
            matcher.appendReplacement(normalized, Matcher.quoteReplacement("{" + toSnakeCase(matcher.group(1)) + "}"));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private static String toSnakeCase(String value) {
        StringBuilder result = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isUpperCase(current)) {
                boolean followsLowerOrDigit = index > 0
                        && (Character.isLowerCase(value.charAt(index - 1)) || Character.isDigit(value.charAt(index - 1)));
                boolean startsWordBeforeLower = index > 0
                        && index + 1 < value.length()
                        && Character.isUpperCase(value.charAt(index - 1))
                        && Character.isLowerCase(value.charAt(index + 1));
                if ((followsLowerOrDigit || startsWordBeforeLower) && result.charAt(result.length() - 1) != '_') {
                    result.append('_');
                }
                result.append(Character.toLowerCase(current));
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}
