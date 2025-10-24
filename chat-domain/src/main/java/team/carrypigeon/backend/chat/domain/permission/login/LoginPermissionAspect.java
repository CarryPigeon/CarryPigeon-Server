package team.carrypigeon.backend.chat.domain.permission.login;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;

@Aspect
@Component
@Slf4j
public class LoginPermissionAspect {

    @SneakyThrows
    @Around("@annotation(team.carrypigeon.backend.chat.domain.permission.login.LoginPermission)")
    public CPResponse loginPermission(ProceedingJoinPoint joinPoint) {
        Object arg = joinPoint.getArgs()[1];
        if (!(arg instanceof CPSession session)){
            throw new RuntimeException("loginPermission: arg is not CPSession");
        }
        if (session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class)==null){
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("user not login");
        }
        return (CPResponse) joinPoint.proceed();
    }
}