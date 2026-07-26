package com.projecthelper.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiResponse<T> {
    String code;
    String message;
    T data;
    String requestId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().code("SUCCESS").message("success").data(data)
                .requestId(RequestIdFilter.currentRequestId()).build();
    }
}
