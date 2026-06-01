package team.carrypigeon.backend.chat.domain.features.auth.support.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 内存型邮箱验证码服务。
 * 职责：为 v1 邮箱验证码登录提供最小本地验证码签发与校验能力。
 * 边界：当前不负责真实邮件投递，只保证同进程内验证码会话语义自洽。
 */
@Component
public class InMemoryEmailVerificationCodeService implements EmailVerificationCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);

    private final Map<String, EmailCodeEntry> issuedCodes = new ConcurrentHashMap<>();
    private final TimeProvider timeProvider;

    public InMemoryEmailVerificationCodeService(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
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
        issuedCodes.put(normalize(email), new EmailCodeEntry(generateCode(), timeProvider.nowInstant().plus(CODE_TTL)));
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
        String normalizedEmail = normalize(email);
        EmailCodeEntry entry = issuedCodes.get(normalizedEmail);
        Instant now = timeProvider.nowInstant();
        if (entry == null || entry.expiresAt().isBefore(now) || !entry.code().equals(code)) {
            throw ProblemException.validationFailed("email code is invalid");
        }
        issuedCodes.remove(normalizedEmail);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    private record EmailCodeEntry(String code, Instant expiresAt) {
    }
}
