package team.carrypigeon.backend.infrastructure.basic.environment;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** 确认恰好选中一个基础设施环境，供启动入口和离线配置检查共用。 */
public final class RuntimeEnvironment {

    private RuntimeEnvironment() {
    }

    public static String selected(Environment environment) {
        List<String> matches = Stream.of(EnvironmentComponent.PROD, EnvironmentComponent.DEV, EnvironmentComponent.TEST)
                .filter(name -> environment.acceptsProfiles(Profiles.of(name)))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Select exactly one infrastructure profile (prod, dev, test), matched " + matches);
        }
        return matches.getFirst();
    }
}
