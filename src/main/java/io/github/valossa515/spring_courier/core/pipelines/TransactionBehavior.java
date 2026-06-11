package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.annotations.Timeout;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline behavior that wraps command execution in a programmatic
 * Spring transaction. Only applies to {@link ICommand} requests —
 * queries pass through unmodified.
 *
 * <p>The transaction is rolled back when the downstream pipeline throws
 * <em>or</em> when it completes with an error {@link Response}, so a
 * failed command never commits.
 *
 * <p>Activated when {@code spring-tx} is on the classpath and
 * {@code spring.courier.transaction.enabled=true} (the default).
 *
 * <p>Runs at order {@code Ordered.HIGHEST_PRECEDENCE + 200}, after
 * logging and validation but before user behaviors.
 *
 * <p><strong>Note:</strong> commands annotated with {@link Timeout} are
 * executed on a separate virtual thread; since Spring transactions are
 * thread-bound, the transaction opened here does not cover such handlers.
 * A warning is logged once per request type in that case.
 *
 * @param <R> request type
 * @param <S> response type
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class TransactionBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionBehavior.class);

    private final PlatformTransactionManager transactionManager;
    private final Set<Class<?>> timeoutWarned =
            ConcurrentHashMap.newKeySet();

    public TransactionBehavior(
            PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public S handle(R request, Next<S> next) {
        if (!(request instanceof ICommand<?>)) {
            return next.invoke();
        }

        warnIfTimeoutAnnotated(request);

        String requestName = request.getClass().getSimpleName();
        logger.debug("[Courier] Starting transaction for {}",
                requestName);

        DefaultTransactionDefinition def =
                new DefaultTransactionDefinition();
        def.setName("courier-" + requestName);
        def.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus status =
                transactionManager.getTransaction(def);
        try {
            S result = next.invoke();
            if (result instanceof Response<?> response
                    && !response.isSuccess()) {
                transactionManager.rollback(status);
                logger.warn("[Courier] Transaction rolled back for {} "
                                + "(error response, status={})",
                        requestName, response.getStatusCode());
                return result;
            }
            transactionManager.commit(status);
            logger.debug("[Courier] Transaction committed for {}",
                    requestName);
            return result;
        } catch (Exception ex) {
            transactionManager.rollback(status);
            logger.warn("[Courier] Transaction rolled back for {}: {}",
                    requestName, ex.getMessage());
            throw ex;
        }
    }

    private void warnIfTimeoutAnnotated(R request) {
        Class<?> requestClass = request.getClass();
        Timeout annotation = requestClass.getAnnotation(Timeout.class);
        // Courier only offloads when the annotation value is positive
        if (annotation != null && annotation.value() > 0
                && timeoutWarned.add(requestClass)) {
            logger.warn("[Courier] {} is annotated with @Timeout and runs "
                            + "on a separate thread; the transaction opened by "
                            + "TransactionBehavior will NOT cover its handler",
                    requestClass.getSimpleName());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }
}
