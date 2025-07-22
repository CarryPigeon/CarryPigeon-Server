package team.carrypigeon.backend.api.chat.domain.structure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 核心通信结构的注册器
 * 插件通信结构应自行实现
 * */
@Slf4j
@Component
public class CPChatStructurePostProcessor implements BeanPostProcessor {
    private final Map<String, CPChatStructure> channelMap = new HashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        CPChatStructureTag annotation = bean.getClass().getAnnotation(CPChatStructureTag.class);
        if (annotation != null&& bean instanceof CPChatStructure cpChatChannel) {
            channelMap.put(annotation.value(), cpChatChannel);
            log.debug("注册消息结构{}，对应的处理类为{}",annotation.value(),bean.getClass().getName());
        }
        return bean;
    }

    @Bean
    public Map<String, CPChatStructure> getChannelMap() {
        return channelMap;
    }
}
