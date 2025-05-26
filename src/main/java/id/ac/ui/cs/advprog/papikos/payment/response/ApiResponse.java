package id.ac.ui.cs.advprog.papikos.payment.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ApiResponse<T> {

    private final int status;  // HTTP status code
    private final String message;
    private final T data;
    private final long timestamp;

    // Private constructor to enforce usage of the builder
    private ApiResponse(Builder<T> builder) {
        this.status = builder.status.value(); // Get integer value from HttpStatus
        this.message = builder.message;
        this.data = builder.data;
        this.timestamp = System.currentTimeMillis();
    }

    // Static factory method to get the builder instance
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // --- The Builder ---
    public static class Builder<T> {
        private HttpStatus status;
        private String message;
        private T data;

        // Private constructor for the builder
        private Builder() {
        }

        public Builder<T> status(HttpStatus status) {
            this.status = status;
            return this; // Return builder for chaining
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        // The final build method creates the ApiResponse instance
        public ApiResponse<T> build() {
            // Basic validation (optional)
            if (this.status == null) {
                // Default to OK if no status is set, or throw exception
                this.status = HttpStatus.OK;
                // Or: throw new IllegalStateException("HTTP status cannot be null");
            }
            return new ApiResponse<>(this);
        }

        // --- Convenience methods for common statuses ---

        public ApiResponse<T> ok(T data) {
            return this.status(HttpStatus.OK)
                    .message("Success")
                    .data(data)
                    .build();
        }

        public ApiResponse<T> created(T data) {
            return this.status(HttpStatus.CREATED)
                    .message("Resource created successfully")
                    .data(data)
                    .build();
        }

        public ApiResponse<T> badRequest(String message) {
            return this.status(HttpStatus.BAD_REQUEST)
                    .message(message)
                    .build(); // Type cast needed if T is different
        }

        public ApiResponse<T> notFound(String message) {
            return this.status(HttpStatus.NOT_FOUND)
                    .message(message)
                    .build();
        }

        public ApiResponse<T> internalError(String message) {
            return this.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message(message)
                    .build();
        }
    }
}