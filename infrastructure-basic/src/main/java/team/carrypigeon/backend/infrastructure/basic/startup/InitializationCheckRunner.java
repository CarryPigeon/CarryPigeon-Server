package team.carrypigeon.backend.infrastructure.basic.startup;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 初始化检查执行器。
 * 职责：在启动阶段统一执行共享初始化检查，并在必需检查失败时中止应用进入可用状态。
 * 边界：只消费 startup 共享契约，不关心具体数据库、缓存或对象存储实现细节。
 */
@Slf4j
@Component
public class InitializationCheckRunner implements SmartInitializingSingleton {

    private final List<InitializationChecker> initializationCheckers;

    public InitializationCheckRunner(List<InitializationChecker> initializationCheckers) {
        this.initializationCheckers = new ArrayList<>(initializationCheckers);
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (initializationCheckers.isEmpty()) {
            log.info("No initialization checks registered, skipping startup validation");
            return;
        }
        log.info("Running {} initialization checks before accepting traffic", initializationCheckers.size());
        for (InitializationChecker initializationChecker : initializationCheckers) {
            runCheck(initializationChecker);
        }
    }

    /**
     * 执行单项初始化检查并统一收口结果、异常和耗时诊断。
     * 失败语义：必需检查失败时中止启动；可选检查失败时记录警告并继续。
     *
     * @param initializationChecker 待执行的初始化检查
     */
    private void runCheck(InitializationChecker initializationChecker) {
        String checkName = initializationChecker.name();
        long startedAtNanos = System.nanoTime();
        InitializationCheckResult result;
        try {
            result = initializationChecker.check();
        } catch (RuntimeException exception) {
            String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            handleFailure(initializationChecker, checkName, message, elapsedMillis(startedAtNanos), exception);
            return;
        }
        long elapsedMillis = elapsedMillis(startedAtNanos);
        if (result == null || !result.passed()) {
            String message = result == null ? "check returned no result" : result.message();
            handleFailure(initializationChecker, checkName, message, elapsedMillis, null);
            return;
        }
        log.info("Initialization check passed [{}] in {} ms: {}", checkName, elapsedMillis, result.message());
    }

    private void handleFailure(
            InitializationChecker initializationChecker,
            String checkName,
            String message,
            long elapsedMillis,
            RuntimeException cause
    ) {
        if (initializationChecker.required()) {
            if (cause == null) {
                log.error("Required initialization check failed [{}] after {} ms: {}",
                        checkName, elapsedMillis, message);
            } else {
                log.error("Required initialization check threw [{}] after {} ms: {}",
                        checkName, elapsedMillis, message, cause);
            }
            throw new IllegalStateException("Required initialization check failed: " + checkName, cause);
        }
        if (cause == null) {
            log.warn("Initialization check failed but marked optional [{}] after {} ms: {}",
                    checkName, elapsedMillis, message);
            return;
        }
        log.warn("Initialization check threw but is marked optional [{}] after {} ms: {}",
                checkName, elapsedMillis, message, cause);
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
