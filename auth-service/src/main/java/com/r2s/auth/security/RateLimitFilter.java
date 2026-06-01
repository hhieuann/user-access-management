package com.r2s.auth.security;

import com.r2s.core.response.ApiResponseWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limit cho /auth/login theo IP.
 *
 * <p>LUU Y PRODUCTION:
 * <ul>
 *   <li>Filter nay luu bucket trong memory cua moi instance.
 *       Khi scale > 1 instance, attacker co the ne limit qua instance khac.
 *       Production goi y dat rate limit o API gateway/ingress, hoac dung Redis chia se.</li>
 *   <li>Da bo sung TTL eviction de tranh map tang vo han.</li>
 *   <li>Da bo sung doc X-Forwarded-For khi chay sau reverse proxy/load balancer.
 *       Set 'app.rate-limit.trusted-proxies' = danh sach IP cua proxy (CSV).</li>
 * </ul>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_MESSAGE =
            "Too many login attempts. Please try again later.";

    /** Map IP -> bucket entry (bucket + lastAccess time). */
    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    /** Background thread don entry het han. */
    private final ScheduledExecutorService cleanupExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "rate-limit-cleanup");
                t.setDaemon(true);
                return t;
            });

    /** TTL: bucket se bi xoa neu khong co request trong 10 phut. */
    private static final Duration BUCKET_TTL = Duration.ofMinutes(10);

    /** Toi da 5 request/phut. */
    private static final int MAX_REQUESTS_PER_MINUTE = 5;

    /** Danh sach proxy tin cay (CSV). Neu rong, KHONG doc X-Forwarded-For. */
    private final List<String> trustedProxies;

    /** Helper ghi ApiResponse format thong nhat (DRY). */
    private final ApiResponseWriter apiResponseWriter;

    public RateLimitFilter(
            @Value("${app.rate-limit.trusted-proxies:}") String trustedProxiesCsv,
            ApiResponseWriter apiResponseWriter) {
        this.trustedProxies = parseTrustedProxies(trustedProxiesCsv);
        this.apiResponseWriter = apiResponseWriter;

        // Don dep bucket het han moi phut.
        cleanupExecutor.scheduleAtFixedRate(
                this::evictExpiredBuckets, 1, 1, TimeUnit.MINUTES);
    }

    private static List<String> parseTrustedProxies(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Bucket createBucket() {
        // Bucket4j 8.x builder API (thay cho Bandwidth.classic + Refill.greedy deprecated)
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS_PER_MINUTE)
                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private BucketEntry getBucket(String ip) {
        BucketEntry entry = buckets.computeIfAbsent(
                ip, k -> new BucketEntry(createBucket(), Instant.now()));
        entry.touch();
        return entry;
    }

    /**
     * Lay client IP that. Neu request den tu trusted proxy thi parse X-Forwarded-For.
     * Mac dinh dung remoteAddr.
     */
    String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.isEmpty() || !trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) return remoteAddr;
        // X-Forwarded-For: client, proxy1, proxy2 -> client la phan tu dau
        int comma = xff.indexOf(',');
        String first = comma >= 0 ? xff.substring(0, comma) : xff;
        return first.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Chi rate limit endpoint login
        if (!"/auth/login".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = getBucket(ip).bucket();

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            // Tra 429 ApiResponse format thong nhat (DRY qua ApiResponseWriter)
            apiResponseWriter.writeError(
                    response, HttpStatus.TOO_MANY_REQUESTS.value(), RATE_LIMIT_MESSAGE);
        }
    }

    /** Don entry chua duoc cham trong BUCKET_TTL phut gan day. */
    void evictExpiredBuckets() {
        Instant cutoff = Instant.now().minus(BUCKET_TTL);
        buckets.entrySet().removeIf(e -> e.getValue().lastAccess().isBefore(cutoff));
    }

    /** So entry hien tai (cho test). */
    int bucketCount() {
        return buckets.size();
    }

    @PreDestroy
    void shutdown() {
        cleanupExecutor.shutdownNow();
    }

    /** Wrapper de track lastAccess cho moi bucket. */
    static final class BucketEntry {
        private final Bucket bucket;
        private volatile Instant lastAccess;

        BucketEntry(Bucket bucket, Instant lastAccess) {
            this.bucket = bucket;
            this.lastAccess = lastAccess;
        }

        Bucket bucket() {
            return bucket;
        }

        Instant lastAccess() {
            return lastAccess;
        }

        void touch() {
            this.lastAccess = Instant.now();
        }
    }
}
