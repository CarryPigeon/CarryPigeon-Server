package team.carrypigeon.backend.api.chat.domain.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息解析器注册后处理器。
 * <p>
 * 自动扫描 `@CPMessageDomain` 标注的 `CPMessageData` 实现，并注册到 `domain -> parser` 映射。
 */
@Slf4j
@Component
public class CPMessageDataPostProcessor implements BeanPostProcessor {

    private final Map<String, CPMessageData> messageDataTypeMaps = new HashMap<>();

    /**
     * 在 Bean 初始化后收集消息解析器。
     *
     * @param bean 已初始化 Bean。
     * @param beanName Bean 名称。
     * @return 原始 Bean。
     * @throws BeansException 当 Bean 生命周期异常时抛出。
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        CPMessageDomain annotation = bean.getClass().getAnnotation(CPMessageDomain.class);
        if (annotation != null && bean instanceof CPMessageData controller) {
            messageDataTypeMaps.put(annotation.value(), controller);
            log.debug("注册消息:{},对应的处理类为{}", annotation.value(), bean.getClass().getName());
        }
        return bean;
    }

    /**
     * 导出消息领域解析器映射。
     *
     * @return 消息领域到解析器的映射。
     */
    @Bean
    public Map<String, CPMessageData> getMessageDataTypeMaps() {
        return messageDataTypeMaps;
    }
}
