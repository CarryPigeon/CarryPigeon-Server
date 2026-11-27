package team.carrypigeon.backend.chat.domain.features.auth.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import team.carrypigeon.backend.chat.domain.shared.valitation.EmailVerificationCode;

public record UpdateCurrentAccountPasswordRequest(
    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "password should not be blank")
    String password,
    @Schema(description = "验证码", example = "123456")
    @EmailVerificationCode
    String code
) {

}
