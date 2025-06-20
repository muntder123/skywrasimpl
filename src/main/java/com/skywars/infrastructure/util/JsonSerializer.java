package com.skywars.infrastructure.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JsonSerializer - Utility class for JSON serialization/deserialization
 * 
 * This class provides a centralized way to handle JSON operations
 * using Jackson library with proper configuration.
 */
public class JsonSerializer {
    
    private static final ObjectMapper objectMapper;
    
    static {
        // Configure ObjectMapper once for the application
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
    }
    
    /**
     * Serialize an object to JSON string
     * 
     * @param object The object to serialize
     * @return JSON string representation
     * @throws JsonProcessingException If serialization fails
     */
    public static String serialize(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }
    
    /**
     * Deserialize a JSON string to an object
     * 
     * @param json JSON string to deserialize
     * @param valueType Class of the target object
     * @return Deserialized object
     * @throws JsonProcessingException If deserialization fails
     */
    public static <T> T deserialize(String json, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.readValue(json, valueType);
    }
    
    /**
     * Get the configured ObjectMapper instance
     * 
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * Serialize an object to pretty-printed JSON string
     * 
     * @param object The object to serialize
     * @return Pretty-printed JSON string
     * @throws JsonProcessingException If serialization fails
     */
    public static String serializePretty(Object object) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }
    
    /**
     * Try to serialize an object, returning null if it fails
     * 
     * @param object The object to serialize
     * @return JSON string or null if serialization fails
     */
    public static String serializeSafe(Object object) {
        try {
            return serialize(object);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * Try to deserialize a JSON string, returning null if it fails
     * 
     * @param json JSON string to deserialize
     * @param valueType Class of the target object
     * @return Deserialized object or null if deserialization fails
     */
    public static <T> T deserializeSafe(String json, Class<T> valueType) {
        try {
            return deserialize(json, valueType);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
