package team.carrypigeon.backend.infrastructure.basic.startup;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import team.carrypigeon.backend.infrastructure.basic.logging.LogValueSanitizer;

/**
 * 初始化检查执行器。
 * 职责：在启动阶段统一执行共享初始化检查，并在必需检查失败时中止应用进入可用状态。
 * 边界：只消费 startup 共享契约，不关心具体数据库、缓存或对象存储实现细节。
 */
@Slf4j
public class InitializationCheckRunner implements SmartInitializingSingleton {

    private final List<InitializationChecker> initializationCheckers;

    public InitializationCheckRunner(List<InitializationChecker> initializationCheckers) {
        this.initializationCheckers = new ArrayList<>(initializationCheckers);
        AnnotationAwareOrderComparator.sort(this.initializationCheckers);
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
        String checkName = safeCheckName(initializationChecker);
        long startedAtNanos = System.nanoTime();
        try {
            InitializationCheckResult result = initializationChecker.check();
            long elapsedMillis = elapsedMillis(startedAtNanos);
            if (result == null) {
                handleFailure(initializationChecker, checkName, "check returned no result", elapsedMillis, null);
                return;
            }
            String message = safeMessage(result.message(), "no message");
            if (result.passed()) {
                log.info("Initialization check passed [{}] in {} ms: {}", checkName, elapsedMillis, message);
                return;
            }
            handleFailure(initializationChecker, checkName, message, elapsedMillis, null);
        } catch (InitializationCheckFailureException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            long elapsedMillis = elapsedMillis(startedAtNanos);
            String message = exception.getClass().getSimpleName() + ": "
                    + safeMessage(exception.getMessage(), "no message");
            handleFailure(initializationChecker, checkName, message, elapsedMillis, exception);
        }
    }

    private void handleFailure(
            InitializationChecker initializationChecker,
            String checkName,
            String message,
            long elapsedMillis,
        RuntimeException cause
    ) {
        if (initializationChecker.required()) {
            if (cause != null) {
                log.error("Required initialization check threw [{}] after {} ms: {}", checkName, elapsedMillis, message);
            }
            throw new InitializationCheckFailureException(checkName, message);
        }
        if (cause == null) {
            log.warn("Initialization check failed but marked optional [{}] after {} ms: {}", checkName, elapsedMillis, message);
        } else {
            log.warn(
                    "Initialization check threw but is marked optional [{}] after {} ms: {}",
                    checkName,
                    elapsedMillis,
                    message
            );
        }
    }

    private String safeCheckName(InitializationChecker initializationChecker) {
        return safeMessage(initializationChecker.name(), "unnamed");
    }

    private String safeMessage(String message, String fallback) {
        String sanitized = LogValueSanitizer.singleLine(message);
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
