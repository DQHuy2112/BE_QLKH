package com.example.auth_service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to build JSON details for before/after change logs.
 */
public final class ChangeLogUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ChangeLogUtils() {
        // utility class
    }

    /**
        * Build a JSON string describing changed fields between two snapshots.
        * Structure:
        * {
        *   "changes": {
        *     "fieldName": { "before": "...", "after": "..." },
        *     ...
        *   }
        * }
        */
    public static String buildChangeDetails(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Map<String, Object>> changes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : before.entrySet()) {
            String key = entry.getKey();
            Object oldValue = entry.getValue();
            Object newValue = after.get(key);

            if (!Objects.equals(oldValue, newValue)) {
                Map<String, Object> diff = new LinkedHashMap<>();
                diff.put("before", oldValue);
                diff.put("after", newValue);
                changes.put(key, diff);
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("changes", changes);

        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            // Fallback: return minimal info; don't break business logic
            return "{\"error\":\"failed_to_build_change_details\"}";
        }
    }
}


