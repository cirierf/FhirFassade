package com.example.demo.config;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
	/**
	 * Pointcut that matches all Spring beans in the application's main packages.
	 */
	@Pointcut("within(com.example.demo.controller..*)" + " || within(com.example.demo.service..*)")
	public void applicationPackagePointcut() {
		// Method is empty as this is just a Pointcut, the implementations are in the
		// advices.
	}

	/**
	 * Retrieves the {@link Logger} associated to the given {@link JoinPoint}.
	 *
	 * @param joinPoint join point we want the logger for.
	 * @return {@link Logger} associated to the given {@link JoinPoint}.
	 */
	private Logger logger(JoinPoint joinPoint) {
		return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
	}

	/**
	 * Advice that logs methods throwing exceptions.
	 *
	 * @param joinPoint join point for advice.
	 * @param e         exception.
	 */
	@AfterThrowing(pointcut = "applicationPackagePointcut()", throwing = "e")
	public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
		logger(joinPoint).error("Exception in {}() with cause = '{}' and exception = '{}'",
				joinPoint.getSignature().getName(), e.getCause() != null ? e.getCause() : "NULL", e.getMessage(), e);
	}

	/**
	 * Advice that logs when a method is entered and exited.
	 *
	 * @param joinPoint join point for advice.
	 * @return result.
	 * @throws Throwable throws {@link IllegalArgumentException}.
	 */
	@Around("applicationPackagePointcut()")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Logger log = logger(joinPoint);
		if (log.isDebugEnabled()) {
			log.debug("Enter: {}() with argument[s] = {}", joinPoint.getSignature().getName(),
					Arrays.toString(joinPoint.getArgs()));
		}
		try {
			Object result = joinPoint.proceed();
			if (log.isDebugEnabled()) {
				log.debug("Exit: {}() with result = {}", joinPoint.getSignature().getName(), result);
			}
			return result;
		} catch (IllegalArgumentException e) {
			log.error("Illegal argument: {} in {}()", Arrays.toString(joinPoint.getArgs()),
					joinPoint.getSignature().getName());
			throw e;
		}
	}
}
