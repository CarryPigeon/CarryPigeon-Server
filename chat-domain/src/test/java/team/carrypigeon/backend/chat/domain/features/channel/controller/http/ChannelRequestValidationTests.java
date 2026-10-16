package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.ChannelApplicationController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.dto.UpdateChannelProfileRequest;

/**
 * 验证频道 HTTP 请求参数的协议层约束。
 */
class ChannelRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 验证频道资料名称沿用创建接口的 128 字符上限。
     */
    @Test
    void updateChannelProfileRequest_nameLongerThan128Characters_isRejected() {
        UpdateChannelProfileRequest request = new UpdateChannelProfileRequest("a".repeat(129), "");

        Set<?> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
                "name length must be less than or equal to 128".equals(getMessage(violation))));
    }

    /**
     * 验证发现接口 limit 的声明式边界为 1 到 50。
     */
    @Test
    void discoverChannels_limit_hasOneToFiftyBounds() throws NoSuchMethodException {
        Method method = ChannelController.class.getDeclaredMethod(
                "discoverChannels", String.class, String.class, int.class,
                team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal.class);
        Parameter limit = method.getParameters()[2];

        Min min = limit.getAnnotation(Min.class);
        Max max = limit.getAnnotation(Max.class);

        assertEquals(1, min.value());
        assertEquals(50, max.value());
    }

    /**
     * 验证审批接口的 channelId 与 applicationId 都必须为正数。
     */
    @Test
    void decideApplication_pathIds_arePositiveValidated() throws NoSuchMethodException {
        Method method = ChannelApplicationController.class.getDeclaredMethod(
                "decideApplication", long.class, long.class,
                team.carrypigeon.backend.chat.domain.features.channel.controller.http.application.dto
                        .DecideChannelApplicationRequest.class,
                team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal.class);

        assertTrue(hasPositiveConstraint(method.getParameters()[0]));
        assertTrue(hasPositiveConstraint(method.getParameters()[1]));
    }

    private static boolean hasPositiveConstraint(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().equals(Positive.class)) {
                return true;
            }
        }
        return false;
    }

    private static String getMessage(Object violation) {
        return ((jakarta.validation.ConstraintViolation<?>) violation).getMessage();
    }
}
