package team.carrypigeon.backend.api.chat.domain.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CPProblemReasonTests {

    @Test
    void fromCode_knownCode_expectedMappedEnum() {
        assertEquals(CPProblemReason.RATE_LIMITED, CPProblemReason.fromCode("rate_limited"));
        assertEquals(CPProblemReason.REQUIRED_PLUGIN_MISSING, CPProblemReason.fromCode("required_plugin_missing"));
        assertEquals(CPProblemReason.API_VERSION_UNSUPPORTED, CPProblemReason.fromCode("api_version_unsupported"));
        assertEquals(CPProblemReason.EVENT_TOO_OLD, CPProblemReason.fromCode("event_too_old"));
    }

    @Test
    void fromCode_unknownCode_expectedInternalError() {
        assertEquals(CPProblemReason.INTERNAL_ERROR, CPProblemReason.fromCode("unknown_reason"));
    }

    @Test
    void reason_statusMapping_expectedStable() {
        assertEquals(406, CPProblemReason.API_VERSION_UNSUPPORTED.status());
        assertEquals(422, CPProblemReason.VALIDATION_FAILED.status());
    }

    @Test
    void of_reasonAndMessage_expectedStatusFromReason() {
        CPProblem problem = CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed");

        assertEquals(422, problem.status());
        assertEquals(CPProblemReason.VALIDATION_FAILED, problem.reason());
        assertEquals("validation failed", problem.message());
    }
}
