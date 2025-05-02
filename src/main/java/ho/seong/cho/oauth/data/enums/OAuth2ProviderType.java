package ho.seong.cho.oauth.data.enums;

import java.util.Arrays;
import java.util.Optional;

/** OAuth2 제공자 타입 */
public enum OAuth2ProviderType {
  APPLE,
  GOOGLE,
  KAKAO,
  NAVER,
  GITHUB,
  FACEBOOK;

  public static Optional<OAuth2ProviderType> findByName(final String providerName) {
    return Arrays.stream(values())
        .filter(type -> type.name().equalsIgnoreCase(providerName))
        .findFirst();
  }
}
