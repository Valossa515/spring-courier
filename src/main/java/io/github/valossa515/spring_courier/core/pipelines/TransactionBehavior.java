package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Pipeline behavior that wraps command execution in a programmatic
 * Spring transaction. Only applies to {@link ICommand} requests —
 * queries pass through unmodified.
 *
 * <p>Activated when {@code spring-tx} is on the classpath and
 * {@code spring.courier.transaction.enabled=true} (the default).
 *
 * <p>Runs at order {@code Ordered.HIGHEST_PRECEDENCE + 200}, after
 * logging and validation but before user behaviors.
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

    public TransactionBehavior(
            PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public S handle(R request, Next<S> next) {
        if (!(request instanceof ICommand<?>)) {
            return next.invoke();
        }

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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }
}
