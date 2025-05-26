package com.nba.stats.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Utility class for converting Redis values to proper Java types
 * Handles the common case where Redis returns values as Object/String
 * but we need them as specific types (int, double, etc.)
 */
@Slf4j
public final class RedisValueConverter {

    private RedisValueConverter() {
        // Utility class - prevent instantiation
    }

    /**
     * Safely convert Redis value to integer from Map<Object, Object> (raw Redis data)
     * @param map The Redis hash map
     * @param key The key to get value for
     * @return Integer value or 0 if conversion fails
     */
    public static int getIntFromRedisMap(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return convertToInt(value);
    }

    /**
     * Safely convert Redis value to integer from Map<Object, Object> with default
     * @param map The Redis hash map
     * @param key The key to get value for
     * @param defaultValue Default value if conversion fails
     * @return Integer value or defaultValue if conversion fails
     */
    public static int getIntFromRedisMap(Map<Object, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return convertToInt(value);
    }

    /**
     * Safely convert Redis value to double from Map<Object, Object> (raw Redis data)
     * @param map The Redis hash map
     * @param key The key to get value for
     * @return Double value or 0.0 if conversion fails
     */
    public static double getDoubleFromRedisMap(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return convertToDouble(value);
    }

    /**
     * Safely convert Redis value to double from Map<Object, Object> with default
     * @param map The Redis hash map
     * @param key The key to get value for
     * @param defaultValue Default value if conversion fails
     * @return Double value or defaultValue if conversion fails
     */
    public static double getDoubleFromRedisMap(Map<Object, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return convertToDouble(value);
    }

    /**
     * Safely convert value to integer from Map<String, Object> (processed data for DB)
     * @param stats Map with String keys (from processed Redis data)
     * @param key The key to get value for
     * @return Integer value or 0 if conversion fails
     */
    public static int getIntFromStatsMap(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        return convertToInt(value);
    }

    /**
     * Safely convert value to double from Map<String, Object> (processed data for DB)
     * @param stats Map with String keys (from processed Redis data)
     * @param key The key to get value for
     * @return Double value or 0.0 if conversion fails
     */
    public static double getDoubleFromStatsMap(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        return convertToDouble(value);
    }

    /**
     * Safely convert any Object to integer
     * @param value The value to convert
     * @return Integer value or 0 if conversion fails
     */
    public static int convertToInt(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number num) {
            return num.intValue();
        }

        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                log.warn("Failed to convert string '{}' to integer, returning 0", str);
                return 0;
            }
        }

        log.warn("Cannot convert value of type {} to integer: {}, returning 0", 
                value.getClass().getSimpleName(), value);
        return 0;
    }

    /**
     * Safely convert any Object to double
     * @param value The value to convert
     * @return Double value or 0.0 if conversion fails
     */
    public static double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number num) {
            return num.doubleValue();
        }

        if (value instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException e) {
                log.warn("Failed to convert string '{}' to double, returning 0.0", str);
                return 0.0;
            }
        }

        log.warn("Cannot convert value of type {} to double: {}, returning 0.0", 
                value.getClass().getSimpleName(), value);
        return 0.0;
    }

    /**
     * Safe division with null checking (commonly used in stats calculations)
     * @param numerator The numerator (can be Object from Redis)
     * @param denominator The denominator
     * @return Division result or 0.0 if invalid
     */
    public static double safeDivide(Object numerator, int denominator) {
        if (numerator == null || denominator == 0) {
            return 0.0;
        }
        
        double num = convertToDouble(numerator);
        return num / denominator;
    }
}