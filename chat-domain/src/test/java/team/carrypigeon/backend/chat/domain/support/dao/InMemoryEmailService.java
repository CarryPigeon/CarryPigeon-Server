package team.carrypigeon.backend.chat.domain.support.dao;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.service.email.CPEmailService;

/**
 * Test double for {@link CPEmailService}.
 * Captures the latest send request for assertions.
 */
@Component
public class InMemoryEmailService implements CPEmailService {

    private volatile String lastTo;
    private volatile String lastSubject;
    private volatile String lastText;

    @Override
    public void sendEmail(String to, String subject, String text) {
        this.lastTo = to;
        this.lastSubject = subject;
        this.lastText = text;
    }

    public String getLastTo() {
        return lastTo;
    }

    public String getLastSubject() {
        return lastSubject;
    }

    public String getLastText() {
        return lastText;
    }

    public void clear() {
        this.lastTo = null;
        this.lastSubject = null;
        this.lastText = null;
    }
}

