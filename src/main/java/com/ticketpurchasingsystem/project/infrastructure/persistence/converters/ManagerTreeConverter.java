package com.ticketpurchasingsystem.project.infrastructure.persistence.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.domain.Utils.ManagerDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class ManagerTreeConverter implements AttributeConverter<Map<String, ManagerDTO>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, ManagerDTO> attribute) {
        if (attribute == null) return "{}";
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public Map<String, ManagerDTO> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new LinkedHashMap<>();
        try {
            return mapper.readValue(dbData, new TypeReference<Map<String, ManagerDTO>>() {});
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}
