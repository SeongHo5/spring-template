package ho.seong.cho.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ho.seong.cho.exception.custom.InternalProcessingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AesGcmStringColumnEncryptorTest {

  private static final String CURRENT_KEY_ID = "local";
  private static final String CURRENT_KEY =
      Base64.getEncoder()
          .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

  @Test
  void encryptAndDecrypt() {
    final var encryptor = newEncryptor();

    final String encrypted = encryptor.encrypt("plain text");

    assertThat(encrypted).startsWith("v1:" + CURRENT_KEY_ID + ":");
    assertThat(encryptor.decrypt(encrypted)).isEqualTo("plain text");
  }

  @Test
  void encryptUsesRandomInitialVector() {
    final var encryptor = newEncryptor();

    final String first = encryptor.encrypt("same text");
    final String second = encryptor.encrypt("same text");

    assertThat(first).isNotEqualTo(second);
    assertThat(encryptor.decrypt(first)).isEqualTo("same text");
    assertThat(encryptor.decrypt(second)).isEqualTo("same text");
  }

  @Test
  void decryptRejectsInvalidFormat() {
    final var encryptor = newEncryptor();

    assertThatThrownBy(() -> encryptor.decrypt("legacy-payload"))
        .isInstanceOf(InternalProcessingException.class)
        .hasMessageContaining("Encrypted data format is invalid.");
  }

  @Test
  void decryptRejectsInvalidBase64Payload() {
    final var encryptor = newEncryptor();

    assertThatThrownBy(() -> encryptor.decrypt("v1:local:not-base64!"))
        .isInstanceOf(InternalProcessingException.class)
        .hasMessageContaining("Base64");
  }

  @Test
  void decryptRejectsTooShortPayload() {
    final var encryptor = newEncryptor();
    final String tooShortPayload = Base64.getEncoder().encodeToString(new byte[12]);

    assertThatThrownBy(() -> encryptor.decrypt("v1:local:" + tooShortPayload))
        .isInstanceOf(InternalProcessingException.class)
        .hasMessageContaining("too short");
  }

  @Test
  void constructorRejectsInvalidKeyLength() {
    final var properties =
        new ColumnEncryptionProperties(
            CURRENT_KEY_ID,
            Map.of(CURRENT_KEY_ID, Base64.getEncoder().encodeToString(new byte[8])));

    assertThatThrownBy(() -> new AesGcmStringColumnEncryptor(properties))
        .isInstanceOf(InternalProcessingException.class)
        .hasMessageContaining("16, 24, or 32 bytes");
  }

  private static AesGcmStringColumnEncryptor newEncryptor() {
    return new AesGcmStringColumnEncryptor(
        new ColumnEncryptionProperties(CURRENT_KEY_ID, Map.of(CURRENT_KEY_ID, CURRENT_KEY)));
  }
}
