package ho.seong.cho.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class StringSetToCsvConverter implements AttributeConverter<Set<String>, String> {

  @Override
  public String convertToDatabaseColumn(Set<String> attribute) {
    if (attribute == null || attribute.isEmpty()) return "";
    return String.join(",", attribute);
  }

  @Override
  public Set<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) return Collections.emptySet();
    return Arrays.stream(dbData.split(","))
        .map(String::trim)
        .filter(v -> !v.isEmpty())
        .collect(Collectors.toSet());
  }
}
