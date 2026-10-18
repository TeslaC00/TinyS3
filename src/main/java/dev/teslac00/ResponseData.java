package dev.teslac00;

import java.nio.charset.StandardCharsets;

public record ResponseData(
        int statusCode,
        byte[] responseBytes
) {

    public static ResponseData fromString(int statusCode, String message) {
        return new ResponseData(statusCode, message.getBytes(StandardCharsets.UTF_8));
    }

    public static ResponseData ok(String message) {
        return new ResponseData(HttpStatus.OK, message.getBytes(StandardCharsets.UTF_8));
    }

    public static ResponseData badRequest(String message) {
        return new ResponseData(HttpStatus.BAD_REQUEST, message.getBytes(StandardCharsets.UTF_8));
    }

}
