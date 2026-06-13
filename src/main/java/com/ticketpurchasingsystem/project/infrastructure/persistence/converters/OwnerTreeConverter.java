package com.ticketpurchasingsystem.project.infrastructure.persistence.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class OwnerTreeConverter implements AttributeConverter<Map<String, OwnerDTO>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, OwnerDTO> attribute) {
        if (attribute == null) return "{}";
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public Map<String, OwnerDTO> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new LinkedHashMap<>();
        try {
            return mapper.readValue(dbData, new TypeReference<Map<String, OwnerDTO>>() {});
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}
