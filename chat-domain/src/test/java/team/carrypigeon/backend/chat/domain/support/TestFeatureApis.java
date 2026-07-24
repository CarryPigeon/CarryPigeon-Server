package team.carrypigeon.backend.chat.domain.support;

import java.time.Clock;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileReferenceDomainApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.service.UserProfileDomainApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 跨 feature 测试 API 工厂。
 * 职责：用稳定 API 包装测试仓储替身，避免测试重新依赖已删除的内部 port 或跨 feature 仓储。
 * 边界：仅供测试源码使用。
 */
public final class TestFeatureApis {

    private TestFeatureApis() {
    }

    public static UserProfileApi userProfiles(UserProfileRepository repository) {
        return new UserProfileDomainApi(
                new UnsupportedAuthAccountApi(),
                repository,
                new NoopEmailVerificationApi(),
                new TimeProvider(Clock.systemUTC()),
                new DirectTransactionRunner()
        );
    }

    public static FileReferenceApi fileReferences() {
        return new FileReferenceDomainApi();
    }

    public static RealtimeEventApi noopRealtime() {
        return command -> {
        };
    }

    private static final class UnsupportedAuthAccountApi implements AuthAccountApi {
        @Override public RegisterResult register(RegisterCommand command) { throw new UnsupportedOperationException(); }
        @Override public String getAccountEmail(long accountId) { throw new UnsupportedOperationException(); }
        @Override public void updateAccountEmail(long accountId, String email) { throw new UnsupportedOperationException(); }
    }

    private static final class NoopEmailVerificationApi implements EmailVerificationApi {
        @Override public void issueCode(team.carrypigeon.backend.chat.domain.features.verification.domain.command.IssueEmailVerificationCodeCommand command) { }
        @Override public void verifyCode(team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand command) { }
    }

    private static final class DirectTransactionRunner implements TransactionRunner {
        @Override public <T> T runInTransaction(java.util.function.Supplier<T> action) { return action.get(); }
        @Override public void runInTransaction(Runnable action) { action.run(); }
    }
}
