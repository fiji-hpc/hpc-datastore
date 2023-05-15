package cz.it4i.fiji.datastore.security;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class PermissionTypeSetConverter implements AttributeConverter<Set<PermissionType>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<PermissionType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .map(Enum::name)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<PermissionType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        String[] values = dbData.split(DELIMITER);
        Set<PermissionType> permissionTypes = EnumSet.noneOf(PermissionType.class);
        for (String value : values) {
            permissionTypes.add(PermissionType.valueOf(value));
        }
        return permissionTypes;
    }
}
