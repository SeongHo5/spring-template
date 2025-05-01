package ho.seong.cho.oauth.support;

import ho.seong.cho.oauth.data.entity.AppleUserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class OpenIdConnectTokenUtils {

  private static final int JWT_PARTS_COUNT = 3;
  private static final String HEADER_KEY_ID = "kid";
  private static final Pattern SPLITTER = Pattern.compile("\\.");

  private OpenIdConnectTokenUtils() {}

  public static AppleUserInfo parse(String idToken, String modulus, String exponent) {
    Claims body = parseTokenClaims(idToken, modulus, exponent).getPayload();
    return AppleUserInfo.fromJwt(body);
  }

  public static String parseKeyIdHeader(
      String idToken, String expectedIssuer, String expectedAudience) {
    return Jwts.parser()
        .requireIssuer(expectedIssuer)
        .requireAudience(expectedAudience)
        .build()
        .parseUnsecuredClaims(extractJwtHeaderAndPayload(idToken))
        .getHeader()
        .get(HEADER_KEY_ID)
        .toString();
  }

  private static Jws<Claims> parseTokenClaims(String token, String modulus, String exponent) {
    return Jwts.parser()
        .verifyWith(generateRSAPublicKey(modulus, exponent))
        .build()
        .parseSignedClaims(token);
  }

  /** Modulus와 Exponent를 이용하여 RSA 공개키를 생성한다. */
  private static PublicKey generateRSAPublicKey(String base64Modulus, String base64Exponent) {
    try {
      byte[] decodedModulus = Base64.getUrlDecoder().decode(base64Modulus);
      byte[] decodedExponent = Base64.getUrlDecoder().decode(base64Exponent);
      var modulus = new BigInteger(1, decodedModulus);
      var exponent = new BigInteger(1, decodedExponent);

      var keySpec = new RSAPublicKeySpec(modulus, exponent);
      return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      log.error("Failed to generate RSA public key / Reason: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /** JWT 토큰의 Header와 Payload를 추출한다. */
  private static String extractJwtHeaderAndPayload(String idToken) {
    String[] parts = SPLITTER.split(idToken);
    if (parts.length != JWT_PARTS_COUNT) {
      throw new RuntimeException("Invalid JWT token format.");
    }
    return parts[0] + "." + parts[1] + ".";
  }
}
