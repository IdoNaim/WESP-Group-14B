package com.ticketpurchasingsystem.project.infrastructure.persistence.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Converter
public class ManagerPermissionsConverter implements AttributeConverter<Map<String, Set<ManagerPermission>>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<Map<String, Set<ManagerPermission>>> TYPE =
            new TypeReference<Map<String, Set<ManagerPermission>>>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Set<ManagerPermission>> attribute) {
        if (attribute == null) return "{}";
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public Map<String, Set<ManagerPermission>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new LinkedHashMap<>();
        try {
            return mapper.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}
