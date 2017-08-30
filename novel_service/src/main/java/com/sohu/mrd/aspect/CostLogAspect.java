package com.sohu.mrd.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by byzuse
 * datetime: 2017/2/28 10:46.
 * 切面示例1
 */
@Aspect
@Component
@Order(0)
public class CostLogAspect {
    private static Logger logger = LogManager.getLogger(CostLogAspect.class);

    private ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(* com.sohu.mrd.controller.*.*(..))")
    public void logCost() {

    }

    @Before("logCost()")
    public void doBefore() {
        long start = System.currentTimeMillis();
        startTime.set(start);
    }

    @AfterReturning(pointcut = "logCost()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        long cost = System.currentTimeMillis() - startTime.get();
        logger.info("[{}:{}] query:'{}', cost:{}", joinPoint.getSignature().getName(), request.getServletPath()
                , request.getQueryString(), cost);
    }


}
