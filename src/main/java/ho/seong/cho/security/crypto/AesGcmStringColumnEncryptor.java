package ho.seong.cho.security.crypto;

import ho.seong.cho.exception.custom.InternalProcessingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** AES/GCM implementation for string column encryption. */
@Service
public class AesGcmStringColumnEncryptor implements StringColumnEncryptor {

  private static final String AES_ALGORITHM = "AES";
  private static final String ENCRYPTION_TRANSFORM = "AES/GCM/NoPadding";
  private static final String FORMAT_VERSION = "v1";

  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / Byte.SIZE;
  private static final int INITIAL_VECTOR_LENGTH = 12;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Base64.Encoder ENCODER = Base64.getEncoder();
  private static final Base64.Decoder DECODER = Base64.getDecoder();

  private final String currentKeyId;
  private final Map<String, SecretKeySpec> keys;

  public AesGcmStringColumnEncryptor(final ColumnEncryptionProperties properties) {
    this.currentKeyId = properties.currentKeyId();
    this.keys = decodeKeys(properties.keys());
  }

  @Override
  public String encrypt(final String plainText) {
    final SecretKeySpec currentKey = getCurrentKey();
    final byte[] initialVector = new byte[INITIAL_VECTOR_LENGTH];
    SECURE_RANDOM.nextBytes(initialVector);

    try {
      final var cipher = initCipher(Cipher.ENCRYPT_MODE, currentKey, initialVector);
      final byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      final byte[] encryptedWithIv =
          ByteBuffer.allocate(initialVector.length + encryptedData.length)
              .put(initialVector)
              .put(encryptedData)
              .array();

      return String.join(
          ":", FORMAT_VERSION, this.currentKeyId, ENCODER.encodeToString(encryptedWithIv));
    } catch (GeneralSecurityException e) {
      throw new InternalProcessingException("Failed to encrypt data.", e);
    }
  }

  @Override
  public String decrypt(final String encryptedText) {
    final var encryptedValue = parseEncryptedValue(encryptedText);
    final SecretKeySpec key = getKey(encryptedValue.keyId());

    try {
      final byte[] decodedData = decodePayload(encryptedValue.payload());
      validatePayloadLength(decodedData);

      final var byteBuffer = ByteBuffer.wrap(decodedData);
      final byte[] initialVector = new byte[INITIAL_VECTOR_LENGTH];
      byteBuffer.get(initialVector);

      final byte[] encryptedData = new byte[byteBuffer.remaining()];
      byteBuffer.get(encryptedData);

      final var cipher = initCipher(Cipher.DECRYPT_MODE, key, initialVector);
      final byte[] decryptedData = cipher.doFinal(encryptedData);
      return new String(decryptedData, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new InternalProcessingException("Failed to decrypt data.", e);
    }
  }

  private SecretKeySpec getCurrentKey() {
    if (!StringUtils.hasText(this.currentKeyId)) {
      throw configurationException("Current encryption key id must be configured.");
    }
    return getKey(this.currentKeyId);
  }

  private SecretKeySpec getKey(final String keyId) {
    final SecretKeySpec key = this.keys.get(keyId);
    if (key == null) {
      throw configurationException("Encryption key is not configured for key id: " + keyId);
    }
    return key;
  }

  private static Map<String, SecretKeySpec> decodeKeys(final Map<String, String> encodedKeys) {
    final Map<String, SecretKeySpec> decodedKeys = new LinkedHashMap<>();
    encodedKeys.forEach(
        (keyId, encodedKey) -> {
          if (!StringUtils.hasText(keyId)) {
            throw configurationException("Encryption key id must not be blank.");
          }
          decodedKeys.put(keyId, decodeKey(keyId, encodedKey));
        });
    return Map.copyOf(decodedKeys);
  }

  private static SecretKeySpec decodeKey(final String keyId, final String encodedKey) {
    if (!StringUtils.hasText(encodedKey)) {
      throw configurationException("Encryption key must not be blank for key id: " + keyId);
    }

    try {
      final byte[] keyBytes = DECODER.decode(encodedKey);
      if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
        throw configurationException(
            "Encryption key must decode to 16, 24, or 32 bytes for key id: " + keyId);
      }
      return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    } catch (IllegalArgumentException e) {
      throw new InternalProcessingException("Encryption key must be Base64 encoded.", e);
    }
  }

  private static EncryptedValue parseEncryptedValue(final String encryptedText) {
    final String[] parts = encryptedText.split(":", 3);
    if (parts.length != 3) {
      throw invalidPayloadException("Encrypted data format is invalid.");
    }
    if (!FORMAT_VERSION.equals(parts[0])) {
      throw invalidPayloadException("Encrypted data version is not supported: " + parts[0]);
    }
    if (!StringUtils.hasText(parts[1])) {
      throw invalidPayloadException("Encrypted data key id is blank.");
    }
    if (!StringUtils.hasText(parts[2])) {
      throw invalidPayloadException("Encrypted data payload is blank.");
    }
    return new EncryptedValue(parts[1], parts[2]);
  }

  private static byte[] decodePayload(final String payload) {
    try {
      return DECODER.decode(payload);
    } catch (IllegalArgumentException e) {
      throw new InternalProcessingException("Encrypted data payload must be Base64 encoded.", e);
    }
  }

  private static void validatePayloadLength(final byte[] payload) {
    if (payload.length < INITIAL_VECTOR_LENGTH + GCM_TAG_LENGTH_BYTES) {
      throw invalidPayloadException("Encrypted data payload is too short.");
    }
  }

  private static Cipher initCipher(
      final int mode, final SecretKeySpec secretKeySpec, final byte[] iv)
      throws GeneralSecurityException {
    final var cipher = Cipher.getInstance(ENCRYPTION_TRANSFORM);
    final var gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
    cipher.init(mode, secretKeySpec, gcmParameterSpec);
    return cipher;
  }

  private static InternalProcessingException configurationException(final String message) {
    return new InternalProcessingException(message, new IllegalStateException(message));
  }

  private static InternalProcessingException invalidPayloadException(final String message) {
    return new InternalProcessingException(message, new IllegalArgumentException(message));
  }

  private record EncryptedValue(String keyId, String payload) {}
}
