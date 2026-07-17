package team.carrypigeon.backend.chat.domain.features.auth.support.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/**
 * 内存型邮箱验证码服务。
 * 职责：为 v1 邮箱验证码登录提供最小本地验证码签发、投递与校验能力。
 * 边界：使用进程内状态保存验证码，不承担 SMTP 等邮件基础设施实现细节。
 */
public class InMemoryEmailVerificationCodeService implements EmailVerificationCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String MAIL_SUBJECT = "CarryPigeon verification code";

    private final Map<String, EmailCodeEntry> issuedCodes = new ConcurrentHashMap<>();
    private final TimeProvider timeProvider;
    private final MailSenderService mailSenderService;

    public InMemoryEmailVerificationCodeService(TimeProvider timeProvider) {
        this(timeProvider, null);
    }

    public InMemoryEmailVerificationCodeService(TimeProvider timeProvider, MailSenderService mailSenderService) {
        this.timeProvider = timeProvider;
        this.mailSenderService = mailSenderService;
    }

    /**
     * 为目标邮箱签发新的验证码。
     * 输入：邮箱地址。
     * 副作用：覆盖该邮箱先前签发的验证码并重置过期时间。
     *
     * @param email 目标邮箱
     */
    @Override
    public void issueCode(String email) {
        String normalizedEmail = normalize(email);
        String code = generateCode();
        issuedCodes.put(normalizedEmail, new EmailCodeEntry(code, timeProvider.nowInstant().plus(CODE_TTL)));
        try {
            deliverCode(normalizedEmail, code);
        } catch (RuntimeException exception) {
            issuedCodes.remove(normalizedEmail);
            throw exception;
        }
    }

    /**
     * 校验邮箱验证码是否有效。
     * 输入：邮箱地址与用户提交的验证码。
     * 副作用：校验成功后立即删除该邮箱的验证码，避免重复使用。
     *
     * @param email 目标邮箱
     * @param code 用户提交的验证码
     */
    @Override
    public void verifyCode(String email, String code) {
        if (code == null || code.isBlank()) {
            throw ProblemException.validationFailed("email code is invalid");
        }
        String normalizedEmail = normalize(email);
        EmailCodeEntry entry = issuedCodes.get(normalizedEmail);
        Instant now = timeProvider.nowInstant();
        if (entry == null || entry.expiresAt().isBefore(now) || !entry.code().equals(code)) {
            throw ProblemException.validationFailed("email code is invalid");
        }
        if (!issuedCodes.remove(normalizedEmail, entry)) {
            throw ProblemException.validationFailed("email code is invalid");
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    /**
     * 投递验证码邮件。
     * 副作用：调用邮件服务发送验证码；邮件服务缺失或发送失败时抛出统一领域问题。
     *
     * @param email 已规范化目标邮箱
     * @param code 六位验证码
     */
    private void deliverCode(String email, String code) {
        if (mailSenderService == null) {
            throw ProblemException.fail("mail_service_unavailable", "mail service is unavailable");
        }
        try {
            mailSenderService.send(new MailSendCommand(email, MAIL_SUBJECT, buildMailText(code)));
        } catch (RuntimeException exception) {
            throw ProblemException.fail("email_delivery_failed", "failed to deliver verification email");
        }
    }

    private String buildMailText(String code) {
        return "Your CarryPigeon verification code is: " + code + "\n\n"
                + "This code will expire in 10 minutes.";
    }

    /**
     * 内存验证码条目。
     * 职责：保存验证码明文和过期时间，仅用于本地内存实现的短期验证流程。
     *
     * @param code 六位邮箱验证码
     * @param expiresAt 验证码过期时间
     */
    private record EmailCodeEntry(String code, Instant expiresAt) {
    }
}
