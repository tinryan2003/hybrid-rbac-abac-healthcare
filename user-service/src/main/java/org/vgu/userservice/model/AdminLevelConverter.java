package org.vgu.userservice.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AdminLevelConverter implements AttributeConverter<Admin.AdminLevel, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Admin.AdminLevel attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public Admin.AdminLevel convertToEntityAttribute(Integer dbData) {
        return Admin.AdminLevel.fromCode(dbData);
    }
}
