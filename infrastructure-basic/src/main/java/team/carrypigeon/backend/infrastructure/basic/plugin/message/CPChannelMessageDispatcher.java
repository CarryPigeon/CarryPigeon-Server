package team.carrypigeon.backend.infrastructure.basic.plugin.message;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CPChannelMessageDispatcher {
    private final Map<String,CPChannelMessageHandler> handlers = new HashMap<>();

    public CPChannelMessageDispatcher(List<CPChannelMessageHandler> beansHandler) {
        beansHandler.forEach((handler) -> {
            MessageHandler annotation = handler.getClass().getAnnotation(MessageHandler.class);
            if (annotation != null) {
                handlers.put(annotation.value(), handler);
            }
        });
    }

    public CPChannelMessageHandler dispatch(String domain) {
        CPChannelMessageHandler handler = handlers.get(domain);
        if (handler == null) {
            throw ProblemException.of(new ProblemMapping() {
                @Override
                public int status() {
                    return 404;
                }

                @Override
                public String reason() {
                    return "no message handler for domain: " + domain;
                }

                @Override
                public String message() {
                    return "no message handler for domain: " + domain;
                }
            });
        }
        return handler;
    }
}
