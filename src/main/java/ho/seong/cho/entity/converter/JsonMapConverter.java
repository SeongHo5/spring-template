package ho.seong.cho.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashMap;
import java.util.Map;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    try {
      return attribute == null ? "{}" : mapper.writeValueAsString(attribute);
    } catch (Exception e) {
      throw new IllegalStateException("JSON serialize error", e);
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    try {
      return dbData == null || dbData.isBlank()
          ? new HashMap<>()
          : mapper.readValue(dbData, new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalStateException("JSON deserialize error", e);
    }
  }
}
