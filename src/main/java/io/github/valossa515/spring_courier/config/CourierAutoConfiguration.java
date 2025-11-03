package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.BehaviorDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração automática dos componentes principais do Spring Courier para o padrão CQRS.
 * <p>
 * Esta classe registra beans necessários para o funcionamento do Courier, incluindo o registro de handlers,
 * o componente de envio de requisições e o pós-processador de descoberta de handlers.
 * </p>
 *
 * <p>Os beans registrados são:</p>
 * <ul>
 *   <li>{@link HandlerRegistry}: Gerencia o registro e localização de handlers.</li>
 *   <li>{@link Courier}: Responsável pelo envio e roteamento de requisições.</li>
 *   <li>{@link HandlerDiscoveryPostProcessor}: Realiza a descoberta automática de handlers no contexto Spring.</li>
 * </ul>
 *
 * @author Valossa515
 * @see HandlerRegistry
 * @see Courier
 * @see HandlerDiscoveryPostProcessor
 */
@Configuration
public class CourierAutoConfiguration{

    @Bean
    public HandlerRegistry handlerRegistry() {
        return new HandlerRegistry();
    }

    @Bean
    public PipelineRegistry pipelineRegistry() {
        return new PipelineRegistry();
    }

    @Bean
    public PipelineExecutor pipelineExecutor(PipelineRegistry pipelineRegistry) {
        return new PipelineExecutor(pipelineRegistry);
    }

    @Bean
    public Courier courier(HandlerRegistry handlerRegistry, PipelineExecutor pipelineExecutor) {
        return new Courier(handlerRegistry, pipelineExecutor);
    }

    @Bean
    public HandlerDiscoveryPostProcessor handlerDiscoveryPostProcessor(
            HandlerRegistry handlerRegistry, ApplicationContext applicationContext) {
        return new HandlerDiscoveryPostProcessor(handlerRegistry, applicationContext);
    }

    @Bean
    public BehaviorDiscoveryPostProcessor behaviorDiscoveryPostProcessor(
            PipelineRegistry pipelineRegistry) {
        return new BehaviorDiscoveryPostProcessor(pipelineRegistry);
    }
}