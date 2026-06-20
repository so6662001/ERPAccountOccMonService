package com.puxun.monitor.schedule;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.lock.DistributedLock;
import com.puxun.monitor.schedule.domain.DetectTask;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态 Cron 调度：启动时加载启用的 CRON 任务并注册触发器；任务变更后可 refresh 重排。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicCronScheduler {

    private final TaskService taskService;
    private final DistributedLock distributedLock;

    private final ThreadPoolTaskScheduler scheduler = init();
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    private static ThreadPoolTaskScheduler init() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(4);
        s.setThreadNamePrefix("detect-cron-");
        s.setWaitForTasksToCompleteOnShutdown(true);
        s.initialize();
        return s;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        refresh();
    }

    /** 取消现有调度并按 DB 中启用的 CRON 任务重新注册。 */
    public synchronized void refresh() {
        futures.values().forEach(f -> f.cancel(false));
        futures.clear();
        for (DetectTask t : taskService.enabledCronTasks()) {
            if (t.getCron() == null || t.getCron().isBlank()) continue;
            try {
                ScheduledFuture<?> future = scheduler.schedule(
                        () -> safeRun(t.getId()), new CronTrigger(t.getCron()));
                if (future != null) futures.put(t.getId(), future);
                log.info("已注册定时任务 {} cron={}", t.getName(), t.getCron());
            } catch (Exception e) {
                log.warn("注册定时任务失败 taskId={}: {}", t.getId(), e.getMessage());
            }
        }
    }

    private void safeRun(Long taskId) {
        String lockKey = "detect:task:" + taskId;
        if (!distributedLock.tryLock(lockKey, Duration.ofMinutes(30))) {
            log.info("任务 {} 正在其他实例执行，跳过本次触发", taskId);
            return;
        }
        try {
            taskService.runNow(taskId, Enums.TriggerType.CRON);
        } catch (Exception e) {
            log.warn("定时执行任务 {} 失败: {}", taskId, e.getMessage());
        } finally {
            distributedLock.unlock(lockKey);
        }
    }
}
