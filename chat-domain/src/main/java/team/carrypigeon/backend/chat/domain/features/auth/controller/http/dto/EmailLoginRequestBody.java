package team.carrypigeon.backend.chat.domain.features.auth.controller.http.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailLoginRequestBody(
    @Schema(description = "邮箱", example = "xxx@xxx.com")
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    String email,
    @Schema(description = "验证码", example = "123456")
    @Pattern(
        regexp = "[0-9]{6}",
        message = "code must be exactly 6 digits"
    )
    String code
) {
}
