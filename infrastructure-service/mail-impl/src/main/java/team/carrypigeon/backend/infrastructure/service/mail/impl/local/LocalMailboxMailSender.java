package team.carrypigeon.backend.infrastructure.service.mail.impl.local;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.springframework.beans.factory.annotation.Value;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/** 开发收件箱，每封邮件写入独立文件，开发者可直接取码。写入失败沿投递回调传播。 */
@EnvironmentComponent(profiles = DEV)
public class LocalMailboxMailSender implements MailSenderService {
    private final Path directory;

    public LocalMailboxMailSender(@Value("${service.mail.local.directory:./target/dev-mailbox}") Path directory) {
        this.directory = directory;
    }

    @Override
    public CompletionStage<Void> send(MailSendCommand command) {
        try {
            Files.createDirectories(directory);
            Path message = Files.createTempFile(directory, "mail-", ".txt");
            Files.writeString(message, "To: " + command.to() + "\nSubject: " + command.subject() + "\n\n" + command.text());
            return CompletableFuture.completedFuture(null);
        } catch (IOException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }
}
