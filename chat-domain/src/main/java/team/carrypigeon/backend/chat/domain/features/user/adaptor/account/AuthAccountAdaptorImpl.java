package team.carrypigeon.backend.chat.domain.features.user.adaptor.account;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.user.domain.capability.AccountProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.capability.AuthAccountAdaptor;

import java.util.List;

@Component
public class AuthAccountAdaptorImpl implements AuthAccountAdaptor {

    private final AuthAccountApi authAccountApi;

    public AuthAccountAdaptorImpl(AuthAccountApi authAccountApi) {
        this.authAccountApi = authAccountApi;
    }

    @Override
    public AccountProfile findById(long userId) {
        AuthAccount account = authAccountApi.getAccount(userId);
        return new AccountProfile(account.id(), account.username(), account.email(), account.createdAt());
    }

    @Override
    public List<AccountProfile> findByIds(List<Long> userIds) {
        return authAccountApi.getAccounts(userIds).stream()
                .map(account -> new AccountProfile(account.id(), account.username(), account.email(), account.createdAt()))
                .toList();
    }
}
