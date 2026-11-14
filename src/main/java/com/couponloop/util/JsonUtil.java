package com.couponloop.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for handling JSON serialization, deserialization, and HTTP response rendering.
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    public static Gson getGson() {
        return GSON;
    }

    public static void sendJson(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        PrintWriter out = response.getWriter();
        out.print(GSON.toJson(data));
        out.flush();
    }

    public static void sendSuccess(HttpServletResponse response, String message, Object data) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", message);
        map.put("data", data);
        sendJson(response, HttpServletResponse.SC_OK, map);
    }

    public static void sendSuccess(HttpServletResponse response, String message) throws IOException {
        sendSuccess(response, message, null);
    }

    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        sendJson(response, statusCode, map);
    }

    public static <T> T parseBody(HttpServletRequest request, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return GSON.fromJson(sb.toString(), clazz);
    }

    public static JsonObject parseJsonObject(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        if (sb.length() == 0) {
            return new JsonObject();
        }
        return GSON.fromJson(sb.toString(), JsonObject.class);
    }
}
