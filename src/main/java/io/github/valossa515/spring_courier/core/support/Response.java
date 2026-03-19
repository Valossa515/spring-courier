package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.CourierException;
import io.github.valossa515.spring_courier.core.exceptions.ValidationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Immutable value object that represents the result of an operation, including
 * optional data, an error message, the success flag, and an HTTP status code.
 *
 * @param <T> response payload type
 */
public class Response<T> {
    private static final String GENERIC_ERROR_MESSAGE = "An internal error occurred";

    private final T data;
    private final String error;
    private final boolean success;
    private final int statusCode;
    private final boolean validationFailure;
    private final String exceptionType;

    // Private constructor to maintain immutability
    private Response(T data, String error, boolean success, int statusCode) {
        this(data, error, success, statusCode, false, null);
    }

    private Response(T data, String error, boolean success, int statusCode,
                     boolean validationFailure) {
        this(data, error, success, statusCode, validationFailure, null);
    }

    private Response(T data, String error, boolean success, int statusCode,
                     boolean validationFailure, String exceptionType) {
        this.data = data;
        this.error = error;
        this.success = success;
        this.statusCode = statusCode;
        this.validationFailure = validationFailure;
        this.exceptionType = exceptionType;
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
     * Creates an error response with message, status code, and an explicit
     * exception type name for metrics tagging.
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static <T> @NotNull Response<T> error(String error, int statusCode,
                                                  @NotNull String exceptionType) {
        return new Response<>(null, error, false, statusCode, false, exceptionType);
    }

    /**
     * Creates a validation error response, marked so that metrics can
     * distinguish pipeline validation failures from other 400 errors.
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static <T> @NotNull Response<T> validationError(String error, int statusCode) {
        return new Response<>(null, error, false, statusCode, true,
                ValidationException.class.getSimpleName());
    }

    /**
     * Creates an error response based on the supplied exception.
     * For security, only {@link CourierException} messages are propagated;
     * all other exception types receive a generic error message to prevent
     * information leakage.
     */
    @Contract("_ -> new")
    public static <T> @NotNull Response<T> error(@NotNull Throwable throwable) {
        String message = (throwable instanceof CourierException)
                ? throwable.getMessage()
                : GENERIC_ERROR_MESSAGE;
        return new Response<>(null, message, false, 500, false,
                resolveExceptionType(throwable));
    }

    /**
     * Creates an error response from an exception and a custom status code.
     * For security, only {@link CourierException} messages are propagated;
     * all other exception types receive a generic error message.
     */
    @Contract("_, _ -> new")
    public static <T> @NotNull Response<T> error(@NotNull Throwable throwable, int statusCode) {
        String message = (throwable instanceof CourierException)
                ? throwable.getMessage()
                : GENERIC_ERROR_MESSAGE;
        return new Response<>(null, message, false, statusCode, false,
                resolveExceptionType(throwable));
    }

    private static @NotNull String resolveExceptionType(@NotNull Throwable throwable) {
        String name = throwable.getClass().getSimpleName();
        return (name == null || name.isBlank()) ? "unknown" : name;
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
     * Indicates whether this error response originated from the
     * pipeline validation layer ({@link io.github.valossa515.spring_courier.core.validation.ValidationBehavior}).
     */
    @JsonIgnore
    public boolean isValidationFailure() {
        return validationFailure;
    }

    /**
     * Returns the simple class name of the exception that caused this error
     * response, or {@code null} if the response was created via the simple
     * string-based factories ({@link #error(String)} / {@link #error(String, int)}).
     *
     * <p>This value is set automatically when a {@link Throwable} is provided,
     * or explicitly by factories such as {@link #error(String, int, String)}
     * and {@link #validationError(String, int)}.
     */
    @JsonIgnore
    public String getExceptionType() {
        return exceptionType;
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
                validationFailure == response.validationFailure &&
                Objects.equals(data, response.data) &&
                Objects.equals(error, response.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, error, success, statusCode, validationFailure);
    }

    @Override
    public String toString() {
        return "Response{" +
                "data=" + (data != null ? "<present>" : "null") +
                ", error='" + error + '\'' +
                ", success=" + success +
                ", statusCode=" + statusCode +
                ", validationFailure=" + validationFailure +
                ", exceptionType='" + exceptionType + '\'' +
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
        private boolean validationFailure;

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

        public Builder<T> validationFailure(boolean validationFailure) {
            this.validationFailure = validationFailure;
            return this;
        }

        public Response<T> build() {
            if (success && error != null) {
                throw new IllegalStateException(
                        "Cannot build a success response with an error message set");
            }
            if (!success && statusCode >= 200 && statusCode < 300) {
                throw new IllegalStateException(
                        "Cannot build an error response with a 2xx status code");
            }
            return new Response<>(data, error, success, statusCode, validationFailure);
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
