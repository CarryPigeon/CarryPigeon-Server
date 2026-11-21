package team.carrypigeon.backend.chat.domain.config.http.advice;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 从 Spring 已报告的错误中选取一个英文提示。字段按 record 声明顺序，数组下标按数值排序。 */
final class ValidationMessages {

    private static final Pattern FIELD_PART = Pattern.compile("[^.\\[\\]]+");
    private static final String FALLBACK = "Request parameter is invalid.";

    private ValidationMessages() {
    }

    static String first(Exception exception) {
        if (exception instanceof BindException binding) {
            return firstField(binding.getTarget(), binding.getFieldErrors());
        }
        if (exception instanceof HandlerMethodValidationException validation) {
            return validation.getParameterValidationResults().stream()
                .sorted(Comparator.comparingInt((ParameterValidationResult result) -> sourceOrder(result.getMethodParameter()))
                    .thenComparingInt(result -> result.getMethodParameter().getParameterIndex())
                    .thenComparing(ParameterValidationResult::getContainerIndex, Comparator.nullsFirst(Integer::compareTo)))
                .map(ValidationMessages::parameterMessage)
                .findFirst().orElse(FALLBACK);
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return mismatch.getName() + " has an invalid value.";
        }
        return FALLBACK;
    }

    private static int sourceOrder(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(PathVariable.class)) {
            return 0;
        }
        return parameter.hasParameterAnnotation(RequestBody.class) ? 2 : 1;
    }

    private static String parameterMessage(ParameterValidationResult result) {
        if (result instanceof ParameterErrors errors) {
            return firstField(result.getArgument(), errors.getFieldErrors());
        }
        return result.getResolvableErrors().stream()
            .min(Comparator.comparingInt(ValidationMessages::constraintOrder)
                .thenComparing(ValidationMessages::message))
            .map(ValidationMessages::message).orElse(FALLBACK);
    }

    private static String firstField(Object target, List<FieldError> errors) {
        return errors.stream()
            .min(Comparator.comparing((FieldError error) -> fieldOrder(target, error.getField()),
                    Arrays::compare)
                .thenComparing(FieldError::getField)
                .thenComparingInt(ValidationMessages::constraintOrder)
                .thenComparing(ValidationMessages::message))
            .map(ValidationMessages::message).orElse(FALLBACK);
    }

    private static int[] fieldOrder(Object target, String field) {
        Matcher parts = FIELD_PART.matcher(field);
        Class<?> type = target == null ? Object.class : target.getClass();
        var order = IntStream.builder();
        while (parts.find()) {
            String part = parts.group();
            if (part.chars().allMatch(Character::isDigit)) {
                order.add(Integer.parseInt(part));
                continue;
            }
            RecordComponent[] fields = type.getRecordComponents();
            int index = 0;
            while (fields != null && index < fields.length && !fields[index].getName().equals(part)) {
                index++;
            }
            order.add(index);
            if (fields != null && index < fields.length) {
                type = ResolvableType.forType(fields[index].getGenericType())
                    .getNested(fields[index].getType() == List.class ? 2 : 1).resolve(Object.class);
            }
        }
        return order.build().toArray();
    }

    private static int constraintOrder(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        String code = codes == null || codes.length == 0 ? "" : codes[codes.length - 1];
        return switch (code) {
            case "NotNull", "NotBlank", "NotEmpty" -> 0;
            case "Email", "Pattern", "typeMismatch" -> 1;
            default -> 2;
        };
    }

    private static String message(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null ? FALLBACK : error.getDefaultMessage();
    }
}
