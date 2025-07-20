package com.tguard.tguard_backend.common;

public record ApiResponse<T>(
        T data,
        String message
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "success");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(null, message);
    }
}
