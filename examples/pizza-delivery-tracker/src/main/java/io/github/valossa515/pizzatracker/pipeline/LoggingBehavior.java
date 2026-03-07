package io.github.valossa515.pizzatracker.pipeline;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingBehavior implements PipelineBehavior<IRequest<Object>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingBehavior.class);

    @Override
    public Object handle(IRequest<Object> request, Next<Object> next) {
        String requestName = request.getClass().getSimpleName();
        logger.info("[PIPELINE] --> Processing: {}", requestName);

        long start = System.currentTimeMillis();
        Object result = next.invoke();
        long elapsed = System.currentTimeMillis() - start;

        logger.info("[PIPELINE] <-- Completed: {} in {}ms", requestName, elapsed);
        return result;
    }
}
