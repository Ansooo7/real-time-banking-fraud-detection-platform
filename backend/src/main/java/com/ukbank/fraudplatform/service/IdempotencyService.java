package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.dto.TransactionResponse;
import com.ukbank.fraudplatform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private final TransactionRepository transactionRepository;
    private final Map<String, TransactionResponse> inMemoryCache = new ConcurrentHashMap<>();
    private static final String IDEMPOTENCY_PREFIX = "idemp:tx:";
    private static final Duration TTL = Duration.ofHours(24);

    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        if (inMemoryCache.containsKey(idempotencyKey)) {
            return true;
        }
        if (redisTemplate != null) {
            try {
                String key = IDEMPOTENCY_PREFIX + idempotencyKey;
                Boolean exists = redisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(exists)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        
        return transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    public void saveKey(String idempotencyKey, TransactionResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        inMemoryCache.put(idempotencyKey, response);
        if (redisTemplate != null) {
            try {
                String key = IDEMPOTENCY_PREFIX + idempotencyKey;
                redisTemplate.opsForValue().set(key, response, TTL);
                log.debug("Stored idempotency key in Redis: {}", idempotencyKey);
            } catch (Exception ignored) {}
        }
    }

    public Optional<TransactionResponse> getCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        if (inMemoryCache.containsKey(idempotencyKey)) {
            return Optional.of(inMemoryCache.get(idempotencyKey));
        }
        if (redisTemplate != null) {
            try {
                String key = IDEMPOTENCY_PREFIX + idempotencyKey;
                Object cached = redisTemplate.opsForValue().get(key);
                if (cached instanceof TransactionResponse txResponse) {
                    return Optional.of(txResponse);
                }
            } catch (Exception ignored) {}
        }
        
        return Optional.empty();
    }
}
