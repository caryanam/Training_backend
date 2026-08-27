package com.training.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LeadStatusConverter implements AttributeConverter<LeadStatus, String> {

    @Override
    public String convertToDatabaseColumn(LeadStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public LeadStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return LeadStatus.fromString(dbData);
    }
}
