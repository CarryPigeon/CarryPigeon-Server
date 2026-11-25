package team.carrypigeon.backend.infrastructure.service.mail.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/** 测试收件箱。测试可提取本次邮件，或指定下一次投递失败以验证验证码作废逻辑。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryMailbox implements MailSenderService {
    private final List<MailSendCommand> inbox = new ArrayList<>();
    private RuntimeException nextFailure;

    @Override
    public synchronized CompletionStage<Void> send(MailSendCommand command) {
        if (nextFailure != null) {
            RuntimeException failure = nextFailure;
            nextFailure = null;
            return CompletableFuture.failedFuture(failure);
        }
        inbox.add(command);
        return CompletableFuture.completedFuture(null);
    }

    public synchronized List<MailSendCommand> drain() {
        var messages = List.copyOf(inbox);
        inbox.clear();
        return messages;
    }

    public synchronized void failNextDelivery(RuntimeException failure) {
        nextFailure = failure;
    }
}
