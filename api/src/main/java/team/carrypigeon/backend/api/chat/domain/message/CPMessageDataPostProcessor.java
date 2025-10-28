package team.carrypigeon.backend.api.chat.domain.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动扫描自定义注解{@link CPMessageDomain}并根据值注入map中
 * */
@Slf4j
@Component
public class CPMessageDataPostProcessor implements BeanPostProcessor {
    private final Map<String, CPMessageData> messageDataTypeMaps = new HashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        CPMessageDomain annotation = bean.getClass().getAnnotation(CPMessageDomain.class);
        if (annotation != null&& bean instanceof CPMessageData controller) {
            messageDataTypeMaps.put(annotation.value(), controller);
            log.debug("注册消息:{},对应的处理类为{}",annotation.value(),bean.getClass().getName());
        }
        return bean;
    }

    @Bean
    public Map<String, CPMessageData> getMessageDataTypeMaps() {
        return messageDataTypeMaps;
    }
}
