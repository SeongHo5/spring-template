package ho.seong.cho.entity.converter;

import static org.assertj.core.api.Assertions.assertThat;

import ho.seong.cho.security.crypto.StringColumnEncryptor;
import org.junit.jupiter.api.Test;

class StringColumnEncryptionConverterTest {

  @Test
  void delegatesEncryptionAndDecryption() {
    final var converter = new StringColumnEncryptionConverter(new StubEncryptor());

    assertThat(converter.convertToDatabaseColumn("plain")).isEqualTo("encrypted:plain");
    assertThat(converter.convertToEntityAttribute("encrypted:plain")).isEqualTo("plain");
  }

  @Test
  void keepsNullAsNull() {
    final var converter = new StringColumnEncryptionConverter(new StubEncryptor());

    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }

  private static class StubEncryptor implements StringColumnEncryptor {

    @Override
    public String encrypt(final String plainText) {
      return "encrypted:" + plainText;
    }

    @Override
    public String decrypt(final String encryptedText) {
      return encryptedText.replace("encrypted:", "");
    }
  }
}
