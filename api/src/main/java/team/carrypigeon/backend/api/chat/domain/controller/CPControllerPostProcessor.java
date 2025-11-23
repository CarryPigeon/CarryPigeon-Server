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
 *
 * */
@Slf4j
@Component
public class CPControllerPostProcessor implements BeanPostProcessor {
    private final Map<String, Class<?>> ControllerAndVoMap = new HashMap<>();
    private final Map<String, Class<?>> ControllerAndResultMap = new HashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        CPControllerTag annotation = bean.getClass().getAnnotation(CPControllerTag.class);
        Class<?> voClazz = annotation.voClazz();
        Class<?> resultClazz = annotation.resultClazz();
        if(voClazz.isAssignableFrom(CPControllerVO.class)&& resultClazz.isAssignableFrom(CPControllerResult.class)){
            ControllerAndVoMap.put(annotation.path(), voClazz);
            ControllerAndResultMap.put(annotation.path(), resultClazz);
            log.debug("注册controller:{},对应的VO为{}",annotation.path(),voClazz.getName());
        }
        return bean;
    }

    @Bean(name = "ControllerAndVOMap")
    public Map<String, Class<?>> getControllerAndVoMap() {
        return ControllerAndVoMap;
    }

    @Bean(name = "ControllerAndResultMap")
    public Map<String, Class<?>> getControllerAndResultMap() {
        return ControllerAndResultMap;
    }
}
