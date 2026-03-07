package io.github.valossa515.pizzatracker.pipeline;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsBehavior implements PipelineBehavior<IRequest<Object>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(MetricsBehavior.class);
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();

    @Override
    public Object handle(IRequest<Object> request, Next<Object> next) {
        String requestName = request.getClass().getSimpleName();
        long count = requestCounts
                .computeIfAbsent(requestName, k -> new AtomicLong(0))
                .incrementAndGet();

        logger.info("[METRICS] {} invoked {} time(s)", requestName, count);

        return next.invoke();
    }

    public Map<String, AtomicLong> getRequestCounts() {
        return Map.copyOf(requestCounts);
    }
}
