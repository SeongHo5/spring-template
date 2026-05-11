package ho.seong.cho.security.crypto;

/** Encrypts and decrypts string values stored in database columns. */
public interface StringColumnEncryptor {

  String encrypt(String plainText);

  String decrypt(String encryptedText);
}
