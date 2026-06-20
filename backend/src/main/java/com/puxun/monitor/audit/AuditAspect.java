package com.puxun.monitor.audit;

import com.puxun.monitor.security.support.AuthUser;
import com.puxun.monitor.security.support.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 审计切面：@Audited 方法成功执行后写审计日志。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(com.puxun.monitor.audit.Audited)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            Method method = sig.getMethod();
            Audited audited = method.getAnnotation(Audited.class);
            AuthUser u = SecurityUtils.currentUser();
            String actor = u == null ? "system" : u.getUsername();
            String role = (u == null || u.getRoles().isEmpty()) ? "-" : String.join(",", u.getRoles());
            String target = audited.target().isBlank() ? method.getName() : audited.target();
            auditService.record(actor, role, audited.action(), target, summarize(pjp), clientIp());
        } catch (Exception e) {
            log.warn("写审计日志失败: {}", e.getMessage());
        }
        return result;
    }

    private String summarize(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return "";
        String s = String.valueOf(args[0]);
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req = attrs.getRequest();
                String xff = req.getHeader("X-Forwarded-For");
                return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "-";
    }
}
