package org.vgu.auditservice.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.vgu.auditservice.enums.ResourceType;

import java.io.IOException;

/**
 * Custom deserializer for ResourceType enum to handle case-insensitive deserialization
 * This fixes the issue where Authorization Service sends "transaction" (lowercase)
 * but the enum expects "TRANSACTION" (uppercase)
 */
public class ResourceTypeDeserializer extends JsonDeserializer<ResourceType> {
    @Override
    public ResourceType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            // Convert to uppercase and try to match enum
            return ResourceType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If not found, default to TRANSACTION
            return ResourceType.TRANSACTION;
        }
    }
}
