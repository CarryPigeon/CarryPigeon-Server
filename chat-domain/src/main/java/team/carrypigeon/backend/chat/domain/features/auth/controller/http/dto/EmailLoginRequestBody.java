package team.carrypigeon.backend.chat.domain.features.auth.controller.http.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import team.carrypigeon.backend.chat.domain.shared.valitation.EmailVerificationCode;

public record EmailLoginRequestBody(
    @Schema(description = "邮箱", example = "xxx@xxx.com")
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    String email,
    @Schema(description = "验证码", example = "123456")
    @EmailVerificationCode
    String code
) {
}
