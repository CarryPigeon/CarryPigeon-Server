package team.carrypigeon.backend.chat.domain.support;

import java.time.Clock;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileReferenceDomainApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.service.UserProfileDomainApi;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
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
                repository,
                new TimeProviderImpl(Clock.systemUTC()),
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

    private static final class DirectTransactionRunner implements TransactionRunner {
        @Override public <T> T runInTransaction(java.util.function.Supplier<T> action) { return action.get(); }
        @Override public void runInTransaction(Runnable action) { action.run(); }
    }
}
