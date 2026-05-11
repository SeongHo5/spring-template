package ho.seong.cho.entity.converter;

import ho.seong.cho.security.crypto.StringColumnEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 문자열 데이터를 AES/GCM 알고리즘을 이용하여 암/복호화하는 JPA {@link AttributeConverter} 구현체 */
@Converter
public class StringColumnEncryptionConverter implements AttributeConverter<String, String> {

  private final StringColumnEncryptor encryptor;

  public StringColumnEncryptionConverter(final StringColumnEncryptor encryptor) {
    this.encryptor = encryptor;
  }

  /**
   * 문자열을 암호화하여 반환합니다.
   *
   * @param attribute 암호화할 원본 문자열
   * @return 암호화된 문자열, 원본 데이터가 {@code null}인 경우 {@code null} 반환
   */
  @Override
  public String convertToDatabaseColumn(final String attribute) {
    if (attribute == null) {
      return null;
    }
    return this.encryptor.encrypt(attribute);
  }

  /**
   * 데이터베이스에서 가져온 암호화된 문자열을 복호화하여 원본 문자열로 변환합니다.
   *
   * @param dbData 복호화할 암호화된 데이터
   * @return 복호화된 원본 문자열, 데이터가 {@code null}인 경우 {@code null} 반환
   */
  @Override
  public String convertToEntityAttribute(final String dbData) {
    if (dbData == null) {
      return null;
    }
    return this.encryptor.decrypt(dbData);
  }
}
