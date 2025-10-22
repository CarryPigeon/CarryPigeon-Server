package team.carrypigeon.backend.chat.domain.permission.login;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

@Aspect
@Component
@Slf4j
public class LoginPermissionAspect {

    @SneakyThrows
    @Around("@annotation(team.carrypigeon.backend.chat.domain.permission.login.LoginPermission)")
    public CPResponse loginPermission(ProceedingJoinPoint joinPoint) {
        Object arg = joinPoint.getArgs()[1];
        if (!(arg instanceof CPSession session)){
            throw new RuntimeException("loginPermission: arg is not CPChannel");
        }
        if (session.getCPUserBO()==null){
            return new CPResponse(CPResponse.ERROR_RESPONSE.getId(),401,null);
        }
        return (CPResponse) joinPoint.proceed();
    }
}