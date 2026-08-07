package com.quantedge.backend.security;

import com.quantedge.backend.entity.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class OneTimeCodeService {

    private final Map<String, User> codeStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public String generateCode(User user) {
        String code = UUID.randomUUID().toString();
        codeStore.put(code, user);

        // Remove code after 1 minute to prevent leaks/replay
        scheduler.schedule(() -> codeStore.remove(code), 1, TimeUnit.MINUTES);

        return code;
    }

    public User exchangeCode(String code) {
        return codeStore.remove(code); // Returns null if not found
    }
}
