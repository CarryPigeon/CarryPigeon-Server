package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

@Component
public class CPChannelMessageDispatcher {
    private final Map<String, CPChannelMessageHandler> handlers = new HashMap<>();

    public CPChannelMessageDispatcher(List<CPChannelMessageHandler> beansHandler) {
        beansHandler.forEach((handler) -> {
            MessageHandler annotation = handler.getClass().getAnnotation(MessageHandler.class);
            if (annotation != null) {
                handlers.put(annotation.value(), handler);
            }
        });
    }

    /**
     * 按消息 domain 获取处理器。
     *
     * @param domain 消息 domain
     * @return 对应处理器
     */
    public CPChannelMessageHandler dispatch(String domain) {
        CPChannelMessageHandler handler = handlers.get(domain);
        if (handler == null) {
            throw ProblemException.of(new ProblemMapping() {
                @Override
                public int code() {
                    return 30008;
                }

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

    /**
     * 判断指定消息 domain 是否已注册处理器。
     *
     * @param domain 消息 domain
     * @return 已注册时返回 true
     */
    public boolean supports(String domain) {
        return domain != null && handlers.containsKey(domain);
    }
}
