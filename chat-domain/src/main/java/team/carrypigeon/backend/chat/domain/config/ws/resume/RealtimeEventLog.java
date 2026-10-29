package team.carrypigeon.backend.chat.domain.config.ws.resume;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.config.ws.RealtimeServerProperties;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时事件窗口。
 * 职责：按接收账号保存最近一段时间的事件帧，供断线续传按 eventId 回放。
 * 边界：只存帧本身，不存接收人集合；不广播、不编码、不判断业务语义。
 */
@Component
public class RealtimeEventLog {

    private final ConcurrentHashMap<Long, AccountLog> logsByAccountId = new ConcurrentHashMap<>();
    private final TimeProvider timeProvider;
    private final long retentionMillis;
    private final int maxEventsPerAccount;
    private final int maxAccounts;

    /**
     * 按运行配置创建事件窗口。
     *
     * @param properties 实时通道配置
     * @param timeProvider 项目统一时间提供器
     */
    @Autowired
    public RealtimeEventLog(RealtimeServerProperties properties, TimeProvider timeProvider) {
        this(
                timeProvider,
                properties.getEventRetention().toMillis(),
                properties.getMaxEventsPerAccount(),
                properties.getMaxEventAccounts()
        );
    }

    public RealtimeEventLog(
            TimeProvider timeProvider,
            long retentionMillis,
            int maxEventsPerAccount,
            int maxAccounts
    ) {
        if (timeProvider == null) {
            throw new IllegalArgumentException("timeProvider must not be null");
        }
        if (retentionMillis <= 0) {
            throw new IllegalArgumentException("retentionMillis must be positive");
        }
        if (maxEventsPerAccount <= 0) {
            throw new IllegalArgumentException("maxEventsPerAccount must be greater than 0");
        }
        if (maxAccounts <= 0) {
            throw new IllegalArgumentException("maxAccounts must be greater than 0");
        }
        this.timeProvider = timeProvider;
        this.retentionMillis = retentionMillis;
        this.maxEventsPerAccount = maxEventsPerAccount;
        this.maxAccounts = maxAccounts;
    }

    /**
     * 追加一条事件帧到每个接收账号的窗口。
     *
     * @param frame 事件帧
     * @param recipients 接收账号
     */
    public void append(RealtimeFrame.Event frame, Collection<Long> recipients) {
        if (frame == null || recipients == null || recipients.isEmpty()) {
            return;
        }
        long nowMillis = timeProvider.nowMillis();
        for (Long accountId : recipients) {
            if (accountId == null) {
                continue;
            }
            AccountLog accountLog = accountLogFor(accountId, nowMillis);
            if (accountLog == null) {
                continue;
            }
            synchronized (accountLog) {
                removeExpired(accountLog, nowMillis);
                if (accountLog.events.size() >= maxEventsPerAccount) {
                    accountLog.events.removeFirst();
                }
                accountLog.events.addLast(new LoggedFrame(frame, nowMillis));
            }
        }
    }

    /**
     * 查询指定事件之后仍在窗口内的事件。
     *
     * @param accountId 账号 ID
     * @param lastEventId 客户端最后确认的事件 ID
     * @return 后续事件；无新事件时为空列表；锚点已不可用时返回 null
     */
    public List<RealtimeFrame.Event> eventsAfter(long accountId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return List.of();
        }
        AccountLog accountLog = logsByAccountId.get(accountId);
        if (accountLog == null) {
            return null;
        }
        synchronized (accountLog) {
            removeExpired(accountLog, timeProvider.nowMillis());
            if (accountLog.events.isEmpty()) {
                logsByAccountId.remove(accountId, accountLog);
                return null;
            }
            boolean anchorFound = false;
            List<RealtimeFrame.Event> events = new ArrayList<>();
            for (LoggedFrame logged : accountLog.events) {
                if (anchorFound) {
                    events.add(logged.frame());
                } else if (logged.frame().eventId().equals(lastEventId)) {
                    anchorFound = true;
                }
            }
            return anchorFound ? List.copyOf(events) : null;
        }
    }

    private AccountLog accountLogFor(long accountId, long nowMillis) {
        AccountLog existing = logsByAccountId.get(accountId);
        if (existing != null) {
            return existing;
        }
        if (logsByAccountId.size() >= maxAccounts) {
            reclaimExpiredAccounts(nowMillis);
        }
        if (logsByAccountId.size() >= maxAccounts) {
            return null;
        }
        return logsByAccountId.computeIfAbsent(accountId, ignored -> new AccountLog());
    }

    /**
     * 回收事件已全部过期的账号窗口。
     * 语义：仅在账号数达到上限且需要为新账号建档时触发，避免在推送主路径上做常规扫描。
     */
    private void reclaimExpiredAccounts(long nowMillis) {
        for (Map.Entry<Long, AccountLog> entry : logsByAccountId.entrySet()) {
            AccountLog accountLog = entry.getValue();
            synchronized (accountLog) {
                removeExpired(accountLog, nowMillis);
                if (accountLog.events.isEmpty()) {
                    logsByAccountId.remove(entry.getKey(), accountLog);
                }
            }
        }
    }

    private void removeExpired(AccountLog accountLog, long nowMillis) {
        long cutoff = nowMillis - retentionMillis;
        while (!accountLog.events.isEmpty() && accountLog.events.getFirst().storedAtMillis() <= cutoff) {
            accountLog.events.removeFirst();
        }
    }

    private static final class AccountLog {
        private final ArrayDeque<LoggedFrame> events = new ArrayDeque<>();
    }

    private record LoggedFrame(RealtimeFrame.Event frame, long storedAtMillis) {
    }
}
