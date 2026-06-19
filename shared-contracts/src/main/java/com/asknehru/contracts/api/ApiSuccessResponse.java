package com.asknehru.contracts.api;

public record ApiSuccessResponse<T>(
        String message,
        T data
) {
}
