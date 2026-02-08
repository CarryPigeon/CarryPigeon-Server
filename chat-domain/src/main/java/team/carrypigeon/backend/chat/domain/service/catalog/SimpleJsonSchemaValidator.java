package team.carrypigeon.backend.chat.domain.service.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 轻量 JSON Schema 校验器（服务端 payload 校验用）。
 * <p>
 * 设计取舍：
 * <ul>
 *   <li>只支持一组“够用的”关键字，用于服务端强校验（拒绝未知字段/非法结构）</li>
 *   <li>不追求完整 JSON Schema 规范覆盖；复杂组合逻辑建议在插件侧编译时约束</li>
 * </ul>
 * <p>
 * 支持关键字（子集实现）：
 * <ul>
 *   <li>{@code $ref}：仅支持本地 {@code #} / {@code #/..}（同一 schema 内 JSON Pointer）</li>
 *   <li>{@code type}（支持数组联合类型；会按 data 选择匹配类型）</li>
 *   <li>{@code const} / {@code enum}</li>
 *   <li>{@code required} / {@code properties} / {@code additionalProperties}</li>
 *   <li>{@code items}</li>
 *   <li>{@code minLength} / {@code maxLength} / {@code pattern}</li>
 *   <li>{@code minimum} / {@code maximum}</li>
 *   <li>{@code minItems} / {@code maxItems}</li>
 *   <li>{@code allOf} / {@code anyOf} / {@code oneOf}</li>
 * </ul>
 * <p>
 * 不支持（示例）：
 * {@code if/then/else}、{@code dependentSchemas}、远程 {@code $ref} 等。
 */
public class SimpleJsonSchemaValidator {

    /**
     * 校验 data 是否满足 schema。
     *
     * @return 错误列表；空列表表示通过
     */
    public List<String> validate(JsonNode schema, JsonNode data) {
        List<String> errors = new ArrayList<>();
        validate(schema, data, "$", errors, schema);
        return errors;
    }

    /**
     * 递归校验入口（内部使用）。
     *
     * @param path 当前节点路径（用于错误定位）
     */
    private void validate(JsonNode schema, JsonNode data, String path, List<String> errors, JsonNode rootSchema) {
        if (schema == null || schema.isNull()) {
            return;
        }

        JsonNode ref = schema.get("$ref");
        if (ref != null && ref.isTextual()) {
            JsonNode resolved = resolveRef(ref.asText(), rootSchema);
            if (resolved == null) {
                // Unsupported ref: treat as validation failure to avoid accepting unknown shapes.
                errors.add(path + ": unsupported $ref");
                return;
            }
            validate(resolved, data, path, errors, rootSchema);
            return;
        }

        // const
        JsonNode constNode = schema.get("const");
        if (constNode != null && !constNode.isNull()) {
            if (!constNode.equals(data)) {
                errors.add(path + ": const");
                return;
            }
        }

        // allOf/anyOf/oneOf (subset)
        if (!validateCompositions(schema, data, path, errors, rootSchema)) {
            return;
        }
        // enum
        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && enumNode.isArray()) {
            boolean ok = false;
            for (JsonNode v : enumNode) {
                if (v != null && v.equals(data)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                errors.add(path + ": not in enum");
                return;
            }
        }

        String type = selectType(schema, data);
        if (type == null) {
            // If type not specified, infer object schema by presence of properties/required.
            if (schema.has("properties") || schema.has("required")) {
                type = "object";
            } else {
                return;
            }
        }

        switch (type) {
            case "object" -> validateObject(schema, data, path, errors, rootSchema);
            case "array" -> validateArray(schema, data, path, errors, rootSchema);
            case "string" -> validateString(schema, data, path, errors);
            case "integer" -> validateInteger(schema, data, path, errors);
            case "number" -> validateNumber(schema, data, path, errors);
            case "boolean" -> {
                if (data == null || !data.isBoolean()) {
                    errors.add(path + ": expected boolean");
                }
            }
            case "null" -> {
                if (data != null && !data.isNull()) {
                    errors.add(path + ": expected null");
                }
            }
            default -> {
                // unsupported type keyword - do not block
            }
        }
    }

    private void validateObject(JsonNode schema, JsonNode data, String path, List<String> errors, JsonNode rootSchema) {
        if (data == null || !data.isObject()) {
            errors.add(path + ": expected object");
            return;
        }

        // required
        Set<String> required = new HashSet<>();
        JsonNode req = schema.get("required");
        if (req != null && req.isArray()) {
            for (JsonNode r : req) {
                if (r != null && r.isTextual()) {
                    required.add(r.asText());
                }
            }
        }
        for (String r : required) {
            if (!data.has(r) || data.get(r).isNull()) {
                errors.add(path + "." + r + ": required");
            }
        }

        // properties
        JsonNode props = schema.get("properties");
        Map<String, JsonNode> propsMap = props != null && props.isObject() ? asFieldsMap(props) : Map.of();

        JsonNode additional = schema.get("additionalProperties");
        boolean additionalAllowed = true;
        JsonNode additionalSchema = null;
        if (additional != null) {
            if (additional.isBoolean()) {
                additionalAllowed = additional.asBoolean();
            } else if (additional.isObject()) {
                additionalAllowed = true;
                additionalSchema = additional;
            }
        }

        Iterator<String> fieldNames = data.fieldNames();
        while (fieldNames.hasNext()) {
            String f = fieldNames.next();
            JsonNode value = data.get(f);
            JsonNode propSchema = propsMap.get(f);
            if (propSchema != null) {
                validate(propSchema, value, path + "." + f, errors, rootSchema);
            } else if (additionalSchema != null) {
                validate(additionalSchema, value, path + "." + f, errors, rootSchema);
            } else if (!additionalAllowed) {
                errors.add(path + "." + f + ": additional properties not allowed");
            }
        }
    }

    private void validateArray(JsonNode schema, JsonNode data, String path, List<String> errors, JsonNode rootSchema) {
        if (data == null || !data.isArray()) {
            errors.add(path + ": expected array");
            return;
        }
        int size = data.size();
        Integer minItems = intOrNull(schema.get("minItems"));
        Integer maxItems = intOrNull(schema.get("maxItems"));
        if (minItems != null && size < minItems) {
            errors.add(path + ": minItems");
        }
        if (maxItems != null && size > maxItems) {
            errors.add(path + ": maxItems");
        }

        JsonNode items = schema.get("items");
        if (items == null) {
            return;
        }
        for (int i = 0; i < size; i++) {
            validate(items, data.get(i), path + "[" + i + "]", errors, rootSchema);
        }
    }

    private void validateString(JsonNode schema, JsonNode data, String path, List<String> errors) {
        if (data == null || !data.isTextual()) {
            errors.add(path + ": expected string");
            return;
        }
        String s = data.asText();
        Integer min = intOrNull(schema.get("minLength"));
        Integer max = intOrNull(schema.get("maxLength"));
        if (min != null && s.length() < min) {
            errors.add(path + ": minLength");
        }
        if (max != null && s.length() > max) {
            errors.add(path + ": maxLength");
        }
        JsonNode patternNode = schema.get("pattern");
        if (patternNode != null && patternNode.isTextual()) {
            Pattern p = Pattern.compile(patternNode.asText());
            if (!p.matcher(s).find()) {
                errors.add(path + ": pattern");
            }
        }
    }

    private void validateInteger(JsonNode schema, JsonNode data, String path, List<String> errors) {
        if (data == null || !data.isIntegralNumber()) {
            errors.add(path + ": expected integer");
            return;
        }
        long v = data.asLong();
        Long min = longOrNull(schema.get("minimum"));
        Long max = longOrNull(schema.get("maximum"));
        if (min != null && v < min) {
            errors.add(path + ": minimum");
        }
        if (max != null && v > max) {
            errors.add(path + ": maximum");
        }
    }

    private void validateNumber(JsonNode schema, JsonNode data, String path, List<String> errors) {
        if (data == null || !data.isNumber()) {
            errors.add(path + ": expected number");
            return;
        }
        double v = data.asDouble();
        Double min = doubleOrNull(schema.get("minimum"));
        Double max = doubleOrNull(schema.get("maximum"));
        if (min != null && v < min) {
            errors.add(path + ": minimum");
        }
        if (max != null && v > max) {
            errors.add(path + ": maximum");
        }
    }

    /**
     * 从 schema.type 中选择一个与 data 匹配的类型。
     * <p>
     * 若 type 为数组，会按顺序选择第一个匹配项。
     */
    private String selectType(JsonNode schema, JsonNode data) {
        JsonNode t = schema.get("type");
        if (t == null || t.isNull()) {
            return null;
        }
        if (t.isTextual()) {
            return t.asText();
        }
        if (!t.isArray()) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        for (JsonNode it : t) {
            if (it != null && it.isTextual()) {
                candidates.add(it.asText());
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        for (String c : candidates) {
            if (matchesType(c, data)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 判断 data 是否匹配某个 JSON Schema type。
     */
    private boolean matchesType(String type, JsonNode data) {
        if (type == null) return false;
        return switch (type) {
            case "object" -> data != null && data.isObject();
            case "array" -> data != null && data.isArray();
            case "string" -> data != null && data.isTextual();
            case "integer" -> data != null && data.isIntegralNumber();
            case "number" -> data != null && data.isNumber();
            case "boolean" -> data != null && data.isBoolean();
            case "null" -> data == null || data.isNull();
            default -> false;
        };
    }

    /**
     * 处理 allOf/anyOf/oneOf 组合关键字。
     *
     * @return true 表示组合关键字通过或未出现；false 表示失败（并已写入 errors）
     */
    private boolean validateCompositions(JsonNode schema, JsonNode data, String path, List<String> errors, JsonNode rootSchema) {
        JsonNode allOf = schema.get("allOf");
        if (allOf != null && allOf.isArray()) {
            for (int i = 0; i < allOf.size(); i++) {
                JsonNode sub = allOf.get(i);
                List<String> subErrors = new ArrayList<>();
                validate(sub, data, path, subErrors, rootSchema);
                if (!subErrors.isEmpty()) {
                    errors.add(path + ": allOf");
                    return false;
                }
            }
        }
        JsonNode anyOf = schema.get("anyOf");
        if (anyOf != null && anyOf.isArray()) {
            boolean ok = false;
            for (int i = 0; i < anyOf.size(); i++) {
                JsonNode sub = anyOf.get(i);
                List<String> subErrors = new ArrayList<>();
                validate(sub, data, path, subErrors, rootSchema);
                if (subErrors.isEmpty()) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                errors.add(path + ": anyOf");
                return false;
            }
        }
        JsonNode oneOf = schema.get("oneOf");
        if (oneOf != null && oneOf.isArray()) {
            int okCount = 0;
            for (int i = 0; i < oneOf.size(); i++) {
                JsonNode sub = oneOf.get(i);
                List<String> subErrors = new ArrayList<>();
                validate(sub, data, path, subErrors, rootSchema);
                if (subErrors.isEmpty()) {
                    okCount++;
                }
            }
            if (okCount != 1) {
                errors.add(path + ": oneOf");
                return false;
            }
        }
        return true;
    }

    /**
     * 解析本地 $ref（同一 schema 内）。
     */
    private JsonNode resolveRef(String ref, JsonNode rootSchema) {
        if (ref == null || ref.isBlank() || rootSchema == null) {
            return null;
        }
        // Only support local refs: "#/..."
        if (!ref.startsWith("#")) {
            return null;
        }
        if (ref.equals("#")) {
            return rootSchema;
        }
        if (!ref.startsWith("#/")) {
            return null;
        }
        String pointer = ref.substring(1);
        try {
            JsonNode n = rootSchema.at(pointer);
            return (n == null || n.isMissingNode()) ? null : n;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, JsonNode> asFieldsMap(JsonNode obj) {
        java.util.LinkedHashMap<String, JsonNode> m = new java.util.LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }

    private Integer intOrNull(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.canConvertToInt()) {
            return n.asInt();
        }
        if (n.isTextual()) {
            try {
                return Integer.parseInt(n.asText());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Long longOrNull(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asLong();
        }
        if (n.isTextual()) {
            try {
                return Long.parseLong(n.asText());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Double doubleOrNull(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asDouble();
        }
        if (n.isTextual()) {
            try {
                return Double.parseDouble(n.asText());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
