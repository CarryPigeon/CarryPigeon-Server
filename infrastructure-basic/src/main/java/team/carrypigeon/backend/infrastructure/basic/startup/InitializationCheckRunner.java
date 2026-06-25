package team.carrypigeon.backend.infrastructure.basic.startup;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * 初始化检查执行器。
 * 职责：在启动阶段统一执行共享初始化检查，并在必需检查失败时中止应用进入可用状态。
 * 边界：只消费 startup 共享契约，不关心具体数据库、缓存或对象存储实现细节。
 */
@Slf4j
public class InitializationCheckRunner implements SmartInitializingSingleton {

    private final List<InitializationCheck> initializationChecks;

    public InitializationCheckRunner(List<InitializationCheck> initializationChecks) {
        this.initializationChecks = new ArrayList<>(initializationChecks);
        AnnotationAwareOrderComparator.sort(this.initializationChecks);
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (initializationChecks.isEmpty()) {
            log.info("No initialization checks registered, skipping startup validation");
            return;
        }
        log.info("Running {} initialization checks before accepting traffic", initializationChecks.size());
        for (InitializationCheck initializationCheck : initializationChecks) {
            InitializationCheckResult result = initializationCheck.check();
            if (result.passed()) {
                log.info("Initialization check passed [{}]: {}", initializationCheck.name(), result.message());
                continue;
            }
            if (initializationCheck.required()) {
                throw new InitializationCheckFailureException(initializationCheck.name(), result.message());
            }
            log.warn("Initialization check failed but marked optional [{}]: {}", initializationCheck.name(), result.message());
        }
    }
}
