package team.carrypigeon.backend.chat.domain.controller.web.api.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpApiPropertiesValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_defaultConfig_expectedNoViolation() {
        CpApiProperties properties = new CpApiProperties();

        var violations = validator.validate(properties);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_authTokenTtlInvalid_expectedViolation() {
        CpApiProperties properties = new CpApiProperties();
        properties.getAuth().setAccessTokenTtlSeconds(0);

        var violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("auth.accessTokenTtlSeconds")));
    }

    @Test
    void validate_pluginScanPathBlank_expectedViolation() {
        CpApiProperties properties = new CpApiProperties();
        properties.getApi().getPluginPackageScan().setDownloadBasePath(" ");

        var violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("api.pluginPackageScan.downloadBasePath")));
    }

    @Test
    void validate_rateLimitWindowInvalid_expectedViolation() {
        CpApiProperties properties = new CpApiProperties();
        properties.getApi().getMessageRateLimit().getCoreText().setMaxRequests(0);

        var violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("api.messageRateLimit.coreText.maxRequests")));
    }
}
