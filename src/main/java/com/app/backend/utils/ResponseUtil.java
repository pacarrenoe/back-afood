package com.app.backend.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class ResponseUtil {

    public static Map<String, Object> status(String code, String message) {
        return Map.of(
                "code", code,
                "message", message
        );
    }

    public static Map<String, Object> successResponse(List<Map<String, Object>> data) {
        return Map.of(
                "status", status("200", "Respuesta exitosa"),
                "result", true,
                "data", data
        );
    }

    public static Map<String, Object> errorResponse(String code, String message) {
        return Map.of(
                "status", status(code, message),
                "result", false,
                "data", null
        );
    }

    public static ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return ResponseEntity.ok(Map.of(
                "status", Map.of(
                        "code", "200",
                        "message", message
                ),
                "result", true,
                "data", List.of(
                        Map.of("retorno", "0", "totalReg", "1", "paginacion", "-1"),
                        data
                )
        ));
    }

    public static ResponseEntity<Map<String, Object>> error(String message, Object detail, int code) {
        return ResponseEntity.status(HttpStatus.valueOf(code)).body(Map.of(
                "status", Map.of(
                        "code", String.valueOf(code),
                        "message", message
                ),
                "result", false,
                "data", List.of(
                        Map.of("retorno", "-1", "paginacion", "-1"),
                        Map.of("error", detail)
                )
        ));
    }
}
