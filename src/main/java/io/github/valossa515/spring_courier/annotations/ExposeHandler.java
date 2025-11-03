package io.github.valossa515.spring_courier.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Anotação para expor um handler como componente Spring no padrão CQRS.
 * <p>
 * Deve ser aplicada em classes que implementam handlers de comandos ou queries,
 * permitindo que sejam detectadas e gerenciadas pelo Spring como beans.
 * O valor opcional pode ser utilizado para definir um nome específico para o bean.
 * </p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>{@code
 * @ExposeHandler("meuHandler")
 * public class MeuHandler implements CommandHandler&lt;MeuComando, MinhaResposta&gt; { ... }
 * }</pre>
 *
 * @see org.springframework.stereotype.Component
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ExposeHandler {
    /**
     * Nome opcional para o bean do handler.
     *
     * @return nome do bean
     */
    String value() default "";
}
