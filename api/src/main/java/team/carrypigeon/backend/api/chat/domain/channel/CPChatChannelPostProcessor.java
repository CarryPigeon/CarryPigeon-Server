package team.carrypigeon.backend.api.chat.domain.channel;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CPChatChannelPostProcessor implements BeanPostProcessor {
    private final Map<String, CPChatChannel> channelMap = new HashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        CPChatChannelTag annotation = bean.getClass().getAnnotation(CPChatChannelTag.class);
        if (annotation != null&& bean instanceof CPChatChannel cpChatChannel) {
            channelMap.put(annotation.value(), cpChatChannel);
        }
        return bean;
    }

    @Bean
    public Map<String, CPChatChannel> getChannelMap() {
        return channelMap;
    }
}
