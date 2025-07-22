package team.carrypigeon.backend.api.chat.domain.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动扫描自定义注解{@link CPControllerTag}并根据值注入map中
 * */
@Slf4j
@Component
public class CPControllerPostProcessor implements BeanPostProcessor {
    private final Map<String, CPController> userMap = new HashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        CPControllerTag annotation = bean.getClass().getAnnotation(CPControllerTag.class);
        if (annotation != null&& bean instanceof CPController controller) {
            userMap.put(annotation.value(), controller);
            log.debug("注册请求controller：{}，对应的处理类为{}",annotation.value(),bean.getClass().getName());
        }
        return bean;
    }

    @Bean
    public Map<String, CPController> getUserMap() {
        return userMap;
    }
}
