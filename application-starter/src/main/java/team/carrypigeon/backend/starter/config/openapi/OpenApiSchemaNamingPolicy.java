package team.carrypigeon.backend.starter.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAPI schema 字段命名策略。
 * 职责：把 springdoc 生成的 Java 属性名统一转换为实际 JSON 协议使用的 snake_case。
 * 边界：只修改文档模型中的属性名、必填字段名和 discriminator 字段名，不改变运行时序列化行为。
 */
final class OpenApiSchemaNamingPolicy {

    private OpenApiSchemaNamingPolicy() {
    }

    /**
     * 规范化 OpenAPI 组件中的全部 schema。
     * 副作用：原地修改 schema 属性键及与其关联的 required、discriminator 元数据。
     *
     * @param openApi 待规范化的 OpenAPI 文档
     */
    static void normalize(OpenAPI openApi) {
        if (openApi == null || openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        openApi.getComponents().getSchemas().values().forEach(schema -> normalizeSchema(schema, visited));
    }

    private static void normalizeSchema(Schema<?> schema, Set<Schema<?>> visited) {
        if (schema == null || !visited.add(schema)) {
            return;
        }

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null && !properties.isEmpty()) {
            Map<String, Schema> normalizedProperties = new LinkedHashMap<>();
            properties.forEach((propertyName, propertySchema) -> {
                String normalizedName = toSnakeCase(propertyName);
                if (normalizedProperties.putIfAbsent(normalizedName, propertySchema) != null) {
                    throw new IllegalStateException("OpenAPI schema property collision after snake_case normalization: "
                            + propertyName + " -> " + normalizedName);
                }
                normalizeSchema(propertySchema, visited);
            });
            schema.setProperties(normalizedProperties);
        }

        List<String> required = schema.getRequired();
        if (required != null && !required.isEmpty()) {
            schema.setRequired(new ArrayList<>(required.stream().map(OpenApiSchemaNamingPolicy::toSnakeCase).toList()));
        }
        if (schema.getDiscriminator() != null && schema.getDiscriminator().getPropertyName() != null) {
            schema.getDiscriminator().setPropertyName(toSnakeCase(schema.getDiscriminator().getPropertyName()));
        }

        normalizeSchema(schema.getItems(), visited);
        normalizeSchemas(schema.getAllOf(), visited);
        normalizeSchemas(schema.getAnyOf(), visited);
        normalizeSchemas(schema.getOneOf(), visited);
        normalizeSchema(schema.getNot(), visited);
        if (schema.getAdditionalProperties() instanceof Schema<?> additionalPropertiesSchema) {
            normalizeSchema(additionalPropertiesSchema, visited);
        }
    }

    private static void normalizeSchemas(List<Schema> schemas, Set<Schema<?>> visited) {
        if (schemas != null) {
            schemas.forEach(schema -> normalizeSchema(schema, visited));
        }
    }

    private static String toSnakeCase(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
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
