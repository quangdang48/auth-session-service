package com.dumy.exception;

public enum ErrorCode {
    SUCCESS(0, "Success"),
    ERROR_400_4000(4000, "Bad request"),
    ERROR_400_VALIDATION(4001, "Validation failed"),
    ERROR_404_2001(2001, "User not found"),
    ERROR_409_2002(2002, "Username already taken"),
    ERROR_401_3001(3001, "Invalid credentials"),
    ERROR_401_3002(3002, "Session not found"),
    ERROR_401_3003(3003, "Session expired"),
    ERROR_401_3004(3004, "Session revoked"),
    ERROR_404_3005(3005, "Tenant not found"),
    ERROR_403_3006(3006, "User is not a member of this tenant"),
    ERROR_401_3008(3008, "Missing session token"),
    ERROR_500_5000(5000, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
