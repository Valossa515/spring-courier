package io.github.valossa515.spring_courier.core.support;

import java.util.Objects;
/**
 * Representa uma resposta genérica para operações, contendo dados, erro, status de sucesso e código de status.
 * <p>
 * Esta classe é imutável e fornece métodos utilitários para criar respostas de sucesso ou erro,
 * além de um builder para casos mais complexos.
 * </p>
 *
 * <p>
 * Principais métodos:
 * <ul>
 *   <li>{@link #success(Object)}: Cria uma resposta de sucesso com dados.</li>
 *   <li>{@link #success(Object, int)}: Cria uma resposta de sucesso com dados e código customizado.</li>
 *   <li>{@link #success()}: Cria uma resposta de sucesso sem dados.</li>
 *   <li>{@link #error(String)}: Cria uma resposta de erro com mensagem.</li>
 *   <li>{@link #error(String, int)}: Cria uma resposta de erro com mensagem e código customizado.</li>
 *   <li>{@link #error(Throwable)}: Cria uma resposta de erro a partir de uma exceção.</li>
 *   <li>{@link #error(Throwable, int)}: Cria uma resposta de erro a partir de uma exceção e código customizado.</li>
 *   <li>{@link #getDataOrThrow()}: Retorna os dados ou lança exceção se houver erro.</li>
 *   <li>{@link #builder()}: Cria um builder para respostas customizadas.</li>
 * </ul>
 * </p>
 *
 * @param <T> Tipo dos dados retornados na resposta
 */
public class Response<T> {
    private final T data;
    private final String error;
    private final boolean success;
    private final int statusCode;

    // Construtor privado para garantir imutabilidade
    private Response(T data, String error, boolean success, int statusCode) {
        this.data = data;
        this.error = error;
        this.success = success;
        this.statusCode = statusCode;
    }

    /**
     * Cria uma resposta de sucesso com dados
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(data, null, true, 200);
    }

    /**
     * Cria uma resposta de sucesso com dados e status code customizado
     */
    public static <T> Response<T> success(T data, int statusCode) {
        return new Response<>(data, null, true, statusCode);
    }

    /**
     * Cria uma resposta de sucesso sem dados
     */
    public static <T> Response<T> success() {
        return new Response<>(null, null, true, 200);
    }

    /**
     * Cria uma resposta de erro com mensagem
     */
    public static <T> Response<T> error(String error) {
        return new Response<>(null, error, false, 500);
    }

    /**
     * Cria uma resposta de erro com mensagem e status code customizado
     */
    public static <T> Response<T> error(String error, int statusCode) {
        return new Response<>(null, error, false, statusCode);
    }

    /**
     * Cria uma resposta de erro a partir de uma exceção
     */
    public static <T> Response<T> error(Throwable throwable) {
        return new Response<>(null, throwable.getMessage(), false, 500);
    }

    /**
     * Cria uma resposta de erro a partir de uma exceção com status code customizado
     */
    public static <T> Response<T> error(Throwable throwable, int statusCode) {
        return new Response<>(null, throwable.getMessage(), false, statusCode);
    }

    // Getters
    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Verifica se a resposta contém dados
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Verifica se a resposta contém erro
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Método utilitário para lançar exceção se a resposta for de erro
     */
    public T getDataOrThrow() {
        if (!success) {
            throw new RuntimeException("Response contains error: " + error);
        }
        return data;
    }

    /**
     * Método utilitário para lançar exceção customizada se a resposta for de erro
     */
    public T getDataOrThrow(RuntimeException exception) {
        if (!success) {
            throw exception;
        }
        return data;
    }

    // equals, hashCode e toString para melhor debugging
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response<?> response = (Response<?>) o;
        return success == response.success &&
                statusCode == response.statusCode &&
                Objects.equals(data, response.data) &&
                Objects.equals(error, response.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, error, success, statusCode);
    }

    @Override
    public String toString() {
        return "Response{" +
                "data=" + data +
                ", error='" + error + '\'' +
                ", success=" + success +
                ", statusCode=" + statusCode +
                '}';
    }

    /**
     * Builder para respostas complexas
     */
    public static class Builder<T> {
        private T data;
        private String error;
        private boolean success;
        private int statusCode = 200;

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> error(String error) {
            this.error = error;
            return this;
        }

        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Response<T> build() {
            return new Response<>(data, error, success, statusCode);
        }
    }

    /**
     * Método estático para criar um builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}
