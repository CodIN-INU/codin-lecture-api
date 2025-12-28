package inu.codin.codin.global.common.aop;

import inu.codin.codin.global.common.constant.LogConstant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class MdcScheduledAspect {

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object applyMdc(ProceedingJoinPoint joinPoint) throws Throwable {
        String txId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(LogConstant.MDC_TX_ID, txId);

        try {

            return joinPoint.proceed();
        } finally {

            MDC.clear();
        }
    }
}
