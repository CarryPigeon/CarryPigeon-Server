package team.carrypigeon.backend.infrastructure.basic.event;

import java.util.function.Consumer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 事务提交后事件发布器。
 * 职责：统一将事件发布动作登记到提交后执行器。
 * 边界：不构造事件、不判断业务语义、不依赖任何领域模块。
 */
@Component
public class AfterCommitEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public AfterCommitEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 登记一条事务成功提交后的 Spring 事件发布动作。
     *
     * @param afterCommit 提交后动作登记器
     * @param event 待发布事件
     */
    public void publishAfterCommit(Consumer<Runnable> afterCommit, Object event) {
        afterCommit.accept(() -> eventPublisher.publishEvent(event));
    }

    /**
     * 立即发布一个已经处于提交后阶段的事件。
     *
     * @param event 待发布事件
     */
    public void publish(Object event) {
        eventPublisher.publishEvent(event);
    }
}
