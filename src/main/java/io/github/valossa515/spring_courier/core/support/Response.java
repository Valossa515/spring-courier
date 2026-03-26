package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.CourierException;
import io.github.valossa515.spring_courier.core.exceptions.ValidationException;
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
    private static final String GENERIC_ERROR_MESSAGE = "An internal error occurred";

    private final T data;
    private final String error;
    private final boolean success;
    private final int statusCode;
    private final boolean validationFailure;
    private final String exceptionType;
    private final Object errorDetails;

    // Private constructor to maintain immutability
    private Response(T data, String error, boolean success, int statusCode) {
        this(data, error, success, statusCode, false, null, null);
    }

    private Response(T data, String error, boolean success, int statusCode,
                     boolean validationFailure) {
        this(data, error, success, statusCode, validationFailure, null, null);
    }

    private Response(T data, String error, boolean success, int statusCode,
                     boolean validationFailure, String exceptionType) {
        this(data, error, success, statusCode, validationFailure, exceptionType, null);
    }

    private Response(T data, String error, boolean success, int statusCode,
                     boolean validationFailure, String exceptionType, Object errorDetails) {
        this.data = data;
        this.error = error;
        this.success = success;
        this.statusCode = statusCode;
        this.validationFailure = validationFailure;
        this.exceptionType = exceptionType;
        this.errorDetails = errorDetails;
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
     * Creates an error response carrying a structured error payload.
     * Useful for returning typed error objects (e.g. a list of field
     * errors, a problem-detail DTO, or a domain-specific error enum).
     *
     * @param error      human-readable error message
     * @param statusCode HTTP status code
     * @param details    structured error payload (any type)
     * @param <T>        response data type (unused in error responses)
     * @param <E>        error details type
     * @return a new error response with the details attached
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static <T, E> @NotNull Response<T> errorWithDetails(
            String error, int statusCode, E details) {
        return new Response<>(null, error, false, statusCode,
                false, null, details);
    }

    /**
     * Creates a validation error response carrying structured
     * error details (e.g. a list of field-level errors).
     *
     * @param error      human-readable error message
     * @param statusCode HTTP status code
     * @param details    structured error payload
     * @param <T>        response data type
     * @param <E>        error details type
     * @return a new validation error response with details
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static <T, E> @NotNull Response<T> validationErrorWithDetails(
            String error, int statusCode, E details) {
        return new Response<>(null, error, false, statusCode, true,
                ValidationException.class.getSimpleName(), details);
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
     * Returns the structured error details, or {@code null} if none
     * were attached. Cast to the expected type at the call site.
     *
     * <pre>{@code
     * List<FieldError> errors = response.getErrorDetails();
     * }</pre>
     *
     * @param <E> the expected error details type
     * @return the error details, or {@code null}
     */
    @SuppressWarnings("unchecked")
    @JsonIgnore
    public <E> E getErrorDetails() {
        return (E) errorDetails;
    }

    /**
     * Returns {@code true} if this response carries structured
     * error details.
     */
    @JsonIgnore
    public boolean hasErrorDetails() {
        return errorDetails != null;
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

    /**
     * Converts this response to a {@link ResponseEntity}.
     * When {@code includeBody} is {@code false} and the response is successful,
     * returns a {@code 204 No Content} response without a body — useful for
     * write operations (commands) that produce no meaningful payload.
     *
     * @param includeBody whether to include the response body on success
     * @return a {@link ResponseEntity} representing this response
     */
    @JsonIgnore
    public ResponseEntity<Response<T>> toEntity(boolean includeBody) {
        if (!includeBody && success) {
            return ResponseEntity.noContent().build();
        }
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
        private Object errorDetails;

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

        public Builder<T> errorDetails(Object errorDetails) {
            this.errorDetails = errorDetails;
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
            return new Response<>(data, error, success, statusCode,
                    validationFailure, null, errorDetails);
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
