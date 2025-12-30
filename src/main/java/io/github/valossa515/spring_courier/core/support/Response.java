package io.github.valossa515.spring_courier.core.support;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

/**
 * Immutable value object that represents the result of an operation, including
 * optional data, an error message, the success flag, and an HTTP status code.
 *
 * @param <T> response payload type
 */
public class Response<T> {
    private final T data;
    private final String error;
    private final boolean success;
    private final int statusCode;

    // Private constructor to maintain immutability
    private Response(T data, String error, boolean success, int statusCode) {
        this.data = data;
        this.error = error;
        this.success = success;
        this.statusCode = statusCode;
    }

    /**
     * Creates a successful response with data.
     */
    @Contract(value = "_ -> new", pure = true)
    public static <T> @NotNull Response<T> success(T data) {
        return new Response<>(data, null, true, 200);
    }

    /**
     * Creates a successful response with data and a custom status code.
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static <T> @NotNull Response<T> success(T data, int statusCode) {
        return new Response<>(data, null, true, statusCode);
    }

    /**
     * Creates a successful response without data.
     */
    @Contract(value = " -> new", pure = true)
    public static <T> @NotNull Response<T> success() {
        return new Response<>(null, null, true, 200);
    }

    /**
     * Creates an error response with the provided message.
     */
    @Contract(value = "_ -> new", pure = true)
    public static <T> @NotNull Response<T> error(String error) {
        return new Response<>(null, error, false, 500);
    }

    /**
     * Creates an error response with the provided message and status code.
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static <T> @NotNull Response<T> error(String error, int statusCode) {
        return new Response<>(null, error, false, statusCode);
    }

    /**
     * Creates an error response based on the supplied exception.
     */
    @Contract("_ -> new")
    public static <T> @NotNull Response<T> error(@NotNull Throwable throwable) {
        return new Response<>(null, throwable.getMessage(), false, 500);
    }

    /**
     * Creates an error response from an exception and a custom status code.
     */
    @Contract("_, _ -> new")
    public static <T> @NotNull Response<T> error(@NotNull Throwable throwable, int statusCode) {
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
     * Indicates whether the response contains data.
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Indicates whether the response contains an error message.
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Throws a specific exception when the response represents an error.
     */
    @JsonIgnore
    public T getDataOrThrow() {
        if (!success) {
            throw new ResponseException(error, statusCode);
        }
        return data;
    }

    /**
     * Throws the provided specific exception when the response represents an error.
     */
    @JsonIgnore
    public void getDataOrThrow(ResponseException exception) {
        if (!success) {
            throw exception;
        }
    }

    @JsonIgnore
    public ResponseEntity<Response<T>> toEntity() {
        return ResponseEntity.status(statusCode).body(this);
    }

    // equals, hashCode, and toString for easier debugging
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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
     * Builder used to create custom response instances.
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
     * Provides a convenient entry point to create a builder.
     */
    @Contract(value = " -> new", pure = true)
    public static <T> @NotNull Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Specific exception for error responses.
     */
    public static class ResponseException extends RuntimeException {
        private final int statusCode;

        public ResponseException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
