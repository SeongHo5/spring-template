package ho.seong.cho.entity.converter;

import jakarta.persistence.AttributeConverter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractSetConverter<T extends Enum<T>>
    implements AttributeConverter<Set<T>, String> {

  /** MySQL SET 타입 데이터가 저장될 때 사용되는 구분자 */
  private static final String SET_DELIMITER = ",";

  private final Class<T> enumClass;

  @Override
  public String convertToDatabaseColumn(Set<T> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    }
    return attributes.stream().map(Enum::name).collect(Collectors.joining(SET_DELIMITER));
  }

  @Override
  public Set<T> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return new HashSet<>(); // 데이터 조작을 허용하기 위해 Set.of 대신 HashSet 사용
    }
    return Arrays.stream(dbData.split(SET_DELIMITER))
        .map(name -> Enum.valueOf(this.enumClass, name))
        .collect(Collectors.toCollection(HashSet::new));
  }
}
